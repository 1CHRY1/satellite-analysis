import pymysql
import requests
import json
import re

# ================= 配置区域 =================
DB_CONFIG = {
    'host': '223.2.34.8',
    'port': 31036,
    'user': 'root',
    'password': '123456',
    'db': 'ard_dev',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# Ollama 设置
LLAMA_API_URL = "http://10.30.88.110:8000/api/generate"
MODEL_NAME = "llama3"

# JSON Params 中需要翻译的字段 Key (注意大小写)
KEYS_TO_TRANSLATE_IN_JSON = ['Description', 'Name']

# 【新增】断点续传设置：从哪个ID之后开始？ (设置为 307 表示从 308 开始跑)
START_FROM_ID = 310

# 测试限制 (设置为 None 则跑全量)
TEST_LIMIT = None
# ===========================================

def clean_llama_output(text):
    """
    通用清洗：去除 Llama3 的废话前缀后缀
    """
    if not text:
        return ""
    
    # 1. 去除常见的开场白
    patterns_start = [
        r"^Here is the translation.*?:",
        r"^Here is the Chinese translation.*?:",
        r"^The translation is:",
        r"^Translation:",
        r"^Sure, here is.*?",
    ]
    for pat in patterns_start:
        text = re.sub(pat, "", text, flags=re.IGNORECASE | re.MULTILINE).strip()

    # 2. 去除常见的结尾废话 (Note: ...)
    patterns_end = [
        r"\n\s*Note:.*",
        r"\n\s*Explanation:.*",
        r"\n\s*\(Note:.*",
        r"\n\s*\* .*? means .*",
    ]
    for pat in patterns_end:
        split_result = re.split(pat, text, flags=re.IGNORECASE | re.DOTALL)
        if len(split_result) > 0:
            text = split_result[0].strip()

    # 3. 去除首尾引号
    text = text.strip('"').strip("'")
    
    return text.strip()

def call_llama(prompt, system_prompt=""):
    """发送请求给 Ollama"""
    payload = {
        "model": MODEL_NAME,
        "prompt": prompt,
        "system": system_prompt,
        "stream": False,
        "options": {
            "temperature": 0.1, 
            "num_predict": 2048 
        }
    }
    try:
        response = requests.post(LLAMA_API_URL, json=payload)
        res_json = response.json()
        raw_text = res_json.get('response', '').strip()
        return clean_llama_output(raw_text)
    except Exception as e:
        print(f"Error calling Llama: {e}")
        return None

def translate_json_recursive(data):
    """递归翻译 JSON 中的特定字段"""
    if isinstance(data, dict):
        for k, v in data.items():
            # 检查 key 是否在列表中 (不区分大小写，增加健壮性)
            if k in KEYS_TO_TRANSLATE_IN_JSON and isinstance(v, str):
                # 针对 UI 短语的翻译 Prompt
                prompt = f"Translate this UI text to Chinese. Output ONLY the translation. Text: {v}"
                trans = call_llama(prompt, "You are a UI translator.")
                
                if trans:
                    # Clean 1: 去除引号和首尾空格
                    cleaned = trans.replace('"', '').strip()
                    # Clean 2: 强制去除末尾的中文句号和英文句号
                    cleaned = cleaned.rstrip('。').rstrip('.')
                    data[k] = cleaned
                    
            elif isinstance(v, (dict, list)):
                translate_json_recursive(v)
    elif isinstance(data, list):
        for item in data:
            translate_json_recursive(item)
    return data

def main():
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cursor:
            # 构建 SQL 语句
            limit_sql = f"LIMIT {TEST_LIMIT}" if TEST_LIMIT else ""
            where_sql = f"WHERE id > {START_FROM_ID}" if START_FROM_ID is not None else ""
            
            print(f"Fetching data (Start ID > {START_FROM_ID}, Limit: {TEST_LIMIT if TEST_LIMIT else 'All'})...")
            
            # 组合 SQL: SELECT ... WHERE id > 307 ORDER BY id ASC
            sql = f"SELECT * FROM methlib_table {where_sql} ORDER BY id ASC {limit_sql}"
            cursor.execute(sql)
            rows = cursor.fetchall()
            
            if not rows:
                print("No data found.")
                return

            print(f"Loaded {len(rows)} records to process.")

            id_map = {} 

            # =====================================================
            # 阶段 1: 翻译 Name (结合 Description 上下文)
            # =====================================================
            print("\n>>> Phase 1: Generating Chinese Names with Context...")
            
            for row in rows:
                row_id = row['id']
                name_en = row['name']
                desc_en = row['description'] if row['description'] else "No description"
                
                # Prompt: 强制只输出中文名称
                prompt_name = f"""
                Task: Translate the method name '{name_en}' into Chinese.
                Method Context(Important Translation Reference): {desc_en}
                
                Rules:
                1. Output ONLY the Chinese name.
                2. Do NOT provide Pinyin or explanations.
                3. Keep it concise.
                """
                
                name_zh = call_llama(prompt_name, "You are a specialized translator. Output only the target string.")
                if name_zh:
                    # 名字也不要有句号
                    name_zh = name_zh.rstrip('。').rstrip('.')
                    
                id_map[row_id] = name_zh
                print(f"[{row_id}] {name_en} -> {name_zh}")

            # =====================================================
            # 阶段 2: 翻译详细内容 (保留英文方法名 & 清洗废话)
            # =====================================================
            print("\n>>> Phase 2: Translating Content & Updating Database...")

            for row in rows:
                row_id = row['id']
                updates = {}
                
                # 1. 填入 Name
                updates['name_zh'] = id_map.get(row_id)
                
                # 2. 翻译 Description (去句号)
                if row['description']:
                    prompt_desc = f"Translate to Chinese: {row['description']}"
                    desc_zh = call_llama(prompt_desc, "Output only the Chinese translation. No notes.")
                    if desc_zh:
                        updates['description_zh'] = desc_zh

                # 3. 翻译 Long Desc (段落文本，保留标点，保留英文方法名)
                if row['long_desc']:
                    prompt_long = f"""
                    Translate the following technical documentation to Chinese.
                    
                    STRICT RULES:
                    1. **Keep all method names, variable names, and code snippets in English.**
                    2. **Do NOT translate the list after 'See Also'**. Keep the 'See Also' line format as: "另请参阅: MethodName1, MethodName2".
                    3. Output **ONLY** the translated text. Do not add "Here is the translation" or any "Notes".
                    4. Do not include Pinyin guides.

                    Original Text:
                    {row['long_desc']}
                    """
                    updates['long_desc_zh'] = call_llama(prompt_long, "You are a technical translator. You strictly follow formatting rules.")

                # 4. 翻译 Params JSON (已在函数内实现去句号)
                if row['params']:
                    try:
                        params_data = row['params']
                        if isinstance(params_data, str):
                            params_data = json.loads(params_data)
                        
                        translated_data = translate_json_recursive(params_data)
                        updates['params_zh'] = json.dumps(translated_data, ensure_ascii=False)
                    except Exception as e:
                        print(f"JSON Error ID {row_id}: {e}")

                # 执行更新 SQL
                if updates:
                    set_clause = ", ".join([f"`{k}`=%s" for k in updates.keys()])
                    values = list(updates.values())
                    values.append(row_id)
                    
                    update_sql = f"UPDATE methlib_table SET {set_clause} WHERE id=%s"
                    cursor.execute(update_sql, values)
                    conn.commit()
                    print(f"Updated ID {row_id}")

    finally:
        if 'conn' in locals() and conn.open:
            conn.close()

if __name__ == "__main__":
    main()