import re

def fix_geom_insert_lines(input_file, output_file):
    with open(input_file, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    fixed_lines = []
    pattern = re.compile(r"(ST_GeomFromText\(\s*'[^']+?'\s*,\s*4326\s*\))")

    for line in lines:
        if line.strip().startswith("INSERT INTO"):
            # 对 ST_GeomFromText 加参数
            def replace_geom(match):
                original = match.group(1)
                if "'axis-order=long-lat'" in original:
                    return original  # 已经有了就不动
                return original[:-1] + ", 'axis-order=long-lat')"  # 插入参数

            fixed_line = pattern.sub(replace_geom, line)
            fixed_lines.append(fixed_line)
        else:
            fixed_lines.append(line)

    with open(output_file, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

# 使用示例
if __name__ == "__main__":
    import os
    
    # 获取当前目录下的sql文件夹路径
    sql_dir = os.path.join(os.path.dirname(__file__), "sql")
    # 创建fixed文件夹
    fixed_dir = os.path.join(os.path.dirname(__file__), "fixed")
    if not os.path.exists(fixed_dir):
        os.makedirs(fixed_dir)
        
    # 遍历sql文件夹下所有sql文件
    for file in os.listdir(sql_dir):
        if file.endswith(".sql"):
            input_file = os.path.join(sql_dir, file)
            output_file = os.path.join(fixed_dir, file)
            fix_geom_insert_lines(input_file, output_file)
