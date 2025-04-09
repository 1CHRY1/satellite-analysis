import time, os , threading
import multiprocessing
import tkinter as tk
from tkinter import filedialog, messagebox
# from tifSlice import process
from bigTifSlicer import process

# --------------------- Multiprocessing ---------------------
def process_band(args):
    """ 处理单个波段 (用于 multiprocessing) """
    input_tif_path, band_output_dir, grid_resolution, clean = args
    process(input_tif_path, band_output_dir, grid_resolution, clean)
    
def process_folder(input_folder, output_root, grid_resolution, clean = False):
    """处理整个影像文件夹，针对每个波段的 TIFF 影像进行切片"""

    if not os.path.isdir(input_folder):
        messagebox.showerror("错误", "输入路径不是有效的文件夹！")
        return

    # 获取所有 .tif 影像文件
    tif_files = [f for f in os.listdir(input_folder) if f.lower().endswith(('.tif', '.tiff'))]
    
    if not tif_files:
        messagebox.showerror("错误", "文件夹内未找到任何 TIFF 影像！")
        return

    # 创建处理任务列表
    tasks = []
    for tif_file in tif_files:
        band_name = os.path.splitext(tif_file)[0]  # 例如 'B01'
        input_tif_path = os.path.join(input_folder, tif_file)
        band_output_dir = os.path.join(output_root, band_name)
        os.makedirs(band_output_dir, exist_ok=True)

        # 任务参数
        tasks.append((input_tif_path, band_output_dir, grid_resolution, clean))

    # 启动多进程
    num_workers = min(len(tasks), multiprocessing.cpu_count())  # 限制进程数
    with multiprocessing.Pool(num_workers) as pool:
        pool.map(process_band, tasks)



# --------------------- Window GUI --------------------------
class RasterTilingGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("栅格切片工具")
        self.root.geometry("400x220")

        # 输入文件路径或目录
        tk.Label(root, text="输入文件/目录:").grid(row=0, column=0, sticky="w", padx=10, pady=5)
        self.input_path_var = tk.StringVar()
        self.input_entry = tk.Entry(root, textvariable=self.input_path_var, width=30, state="readonly")
        self.input_entry.grid(row=0, column=1, columnspan=2)

        self.btn_select_tif = tk.Button(root, text="选择 TIF 文件", command=self.select_tif)
        self.btn_select_tif.grid(row=1, column=1, pady=5)

        self.btn_select_dir = tk.Button(root, text="选择影像目录", command=self.select_directory)
        self.btn_select_dir.grid(row=1, column=2, pady=5)

        # 切片大小
        tk.Label(root, text="切片大小 (km):").grid(row=2, column=0, sticky="w", padx=10, pady=5)
        self.grid_resolution = tk.DoubleVar(value=5.0)
        self.grid_resolution_entry = tk.Entry(root, textvariable=self.grid_resolution, width=10)
        self.grid_resolution_entry.grid(row=2, column=1, sticky="w")
        
        # 复选框：是否删除无效值切片
        self.delete_invalid_tiles = tk.BooleanVar(value=True)
        self.chk_delete_invalid = tk.Checkbutton(root, text="剔除无效值", variable=self.delete_invalid_tiles, command=self.warn_delete_invalid)
        self.chk_delete_invalid.grid(row=2, column=2, columnspan=2, pady=5, padx=1, sticky="w")



        # 输出文件路径
        tk.Label(root, text="输出目录:").grid(row=3, column=0, sticky="w", padx=10, pady=5)
        self.output_path_var = tk.StringVar()
        self.output_entry = tk.Entry(root, textvariable=self.output_path_var, width=30, state="readonly")
        self.output_entry.grid(row=3, column=1, columnspan=2)

        self.btn_select_output = tk.Button(root, text="选择", command=self.select_output)
        self.btn_select_output.grid(row=3, column=3)

        # 运行按钮
        self.btn_start = tk.Button(root, text="开始切片", command=self.start_tif_slicing_thread, bg="green", fg="white")
        self.btn_start.grid(row=4, column=1, pady=10)

        # 状态显示
        self.status_label = tk.Label(root, text="", fg="blue")
        self.status_label.grid(row=5, column=1, pady=5)
        
        # 计数器用于动画循环
        self.running = False  # 处理状态

    def select_tif(self):
        """ 选择单个 TIFF 文件 """
        path = filedialog.askopenfilename(filetypes=[("TIFF files", "*.tif")])
        if path:
            self.input_path_var.set(path)


    def select_directory(self):
        """ 选择影像目录 """
        path = filedialog.askdirectory()
        if path:
            self.input_path_var.set(path)


    def select_output(self):
        """ 选择输出目录 """
        path = filedialog.askdirectory()
        if path:
            self.output_path_var.set(path)


    def warn_delete_invalid(self):
        """ 当用户勾选‘删除无效值切片’时，弹出警告 """
        if self.delete_invalid_tiles.get():
            messagebox.showwarning("注意", "删除无效值切片会增加处理时间，请耐心等待！")

    def start_tif_slicing_thread(self):
        """ 创建新线程执行切片，并在开始时冻结 UI，结束后解冻 """
        self.freeze_ui()
        threading.Thread(target=self.start_tif_slicing, daemon=True).start()


    def freeze_ui(self):
        """ 禁用所有输入控件，防止用户操作 """
        self.btn_select_tif.config(state="disabled")
        self.btn_select_dir.config(state="disabled")
        self.grid_resolution_entry.config(state="disabled")
        self.btn_select_output.config(state="disabled")
        self.btn_start.config(state="disabled")
        self.status_label.config(text="正在切片，请稍候...", fg="blue")
        self.root.update_idletasks()


    def unfreeze_ui(self):
        """ 重新启用 UI 控件 """
        self.btn_select_tif.config(state="normal")
        self.btn_select_dir.config(state="normal")
        self.grid_resolution_entry.config(state="normal")
        self.btn_select_output.config(state="normal")
        self.btn_start.config(state="normal")


    def start_tif_slicing(self):
        """ 执行切片操作 """
        input_path = self.input_path_var.get()
        grid_resolution = self.grid_resolution.get()
        output_path = self.output_path_var.get()

        if not input_path:
            messagebox.showerror("错误", "请输入 TIFF 文件或影像目录！")
            self.unfreeze_ui()
            return
        if not output_path:
            messagebox.showerror("错误", "请选择输出目录！")
            self.unfreeze_ui()
            return

        # 计时
        start_time = time.time()
        self.running = True
        
        is_folder_input = os.path.isdir(input_path)
        should_delete_invalid = self.delete_invalid_tiles.get()
        
        # Core
        if is_folder_input:
            process_folder(input_path, output_path, grid_resolution, should_delete_invalid)
            pass
        else:
            process(input_path, output_path, grid_resolution, should_delete_invalid)

        # 计算耗时
        elapsed_time = time.time() - start_time

        # 更新状态
        self.running = False
        self.status_label.config(text=f"切片完成！耗时 {elapsed_time:.2f} 秒", fg="green")
        messagebox.showinfo("完成", f"切片任务已完成！\n总耗时: {elapsed_time:.2f} 秒")

        # 重新启用 UI
        self.unfreeze_ui()

    



# --------------------- Main --------------------------------
if __name__ == "__main__":
    multiprocessing.freeze_support()
    root = tk.Tk()
    app = RasterTilingGUI(root)
    root.mainloop()