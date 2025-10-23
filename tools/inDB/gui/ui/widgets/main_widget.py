# ui/widgets/config_and_operation_widget.py
import shlex
import tempfile
from PyQt5.QtGui import QIntValidator
from PyQt5.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout, QLabel, QPushButton, QFileDialog,
    QLineEdit, QListWidget, QTextEdit, QGroupBox, QMessageBox, QSizePolicy
)
from PyQt5.QtCore import Qt
import os
import subprocess


class MainWidget(QWidget):
    """综合配置 + 操作区"""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.validator_path = None
        self.mparallel_path = None
        self.current_log_path = None
        self._product_files = []
        self.initUI()

    def initUI(self):
        main_layout = QVBoxLayout(self)
        main_layout.setSpacing(20)
        main_layout.setContentsMargins(15, 10, 15, 10)
        self.validator_path = os.path.join(os.getcwd(), "validator.exe")
        self.mparallel_path = os.path.join(os.getcwd(), "mparallel.exe")

        # ================== 第一部分：配置输入 ==================
        config_group = QGroupBox("配置输入区")
        config_layout = QVBoxLayout(config_group)
        config_layout.setSpacing(12)

        # ---------- product_config ----------
        title1 = QLabel("🧩 product_config 文件（可多选）")
        title1.setStyleSheet("font-weight: bold;")
        self.product_list = QListWidget()
        self.product_list.setFixedHeight(60)
        self.product_list.setSelectionMode(QListWidget.NoSelection)
        btn1 = QPushButton("选择 JSON 文件")
        btn1.clicked.connect(self.on_select_products)
        config_layout.addWidget(title1)
        config_layout.addWidget(self.product_list)
        config_layout.addWidget(btn1)

        # ---------- scenes_dir ----------
        scene_row = QHBoxLayout()
        label2 = QLabel("🌄 scenes_dir:")
        label2.setFixedWidth(100)
        label2.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        self.scene_input = QLineEdit()
        self.scene_input.setPlaceholderText("请选择一个文件夹")
        btn2 = QPushButton("浏览...")
        btn2.clicked.connect(self.on_select_scene)
        scene_row.addWidget(label2)
        scene_row.addWidget(self.scene_input)
        scene_row.addWidget(btn2)
        config_layout.addLayout(scene_row)

        # ---------- db_config + output_log ----------
        db_out_row = QHBoxLayout()

        label3 = QLabel("🗃️ db_config：")
        label3.setFixedWidth(100)
        label3.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        self.db_input = QLineEdit()
        self.db_input.setPlaceholderText("请选择 JSON 文件")
        btn3 = QPushButton("浏览...")
        btn3.clicked.connect(self.on_select_db)

        label4 = QLabel("📝 output_log：")
        label4.setFixedWidth(90)
        label4.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        self.output_input = QLineEdit()
        self.output_input.setPlaceholderText("请选择输出文件")
        btn4 = QPushButton("选择...")
        btn4.clicked.connect(self.on_select_output)
        self.btn_reset = QPushButton("重置")
        self.btn_reset.clicked.connect(self.on_reset_clicked)

        db_out_row.addWidget(label3)
        db_out_row.addWidget(self.db_input, 1)
        db_out_row.addWidget(btn3)
        db_out_row.addSpacing(20)
        db_out_row.addWidget(label4)
        db_out_row.addWidget(self.output_input, 1)
        db_out_row.addWidget(btn4)
        db_out_row.addWidget(self.btn_reset)

        config_layout.addLayout(db_out_row)
        main_layout.addWidget(config_group)

        # ================== 第二部分：操作控制 ==================
        op_group = QGroupBox("操作区")
        op_layout = QVBoxLayout(op_group)
        op_layout.setSpacing(15)

        # --- 第一行：检验 + 打开日志 ---
        top_row = QHBoxLayout()
        self.btn_validate = QPushButton("执行检验")
        self.btn_validate.clicked.connect(self.on_validate_clicked)

        self.btn_open_log = QPushButton("打开日志")
        self.btn_open_log.setEnabled(False)
        self.btn_open_log.clicked.connect(self.on_open_log_clicked)
        top_row.addWidget(self.btn_validate)
        top_row.addWidget(self.btn_open_log)
        top_row.addStretch(1)

        # --- 第二行：inDB 路径输入 ---
        db_row = QHBoxLayout()
        lbl_inDB = QLabel("inDB 程序路径：")
        self.txt_inDB = QLineEdit()
        self.txt_inDB.setPlaceholderText("请输入 inDB 可执行文件路径")
        self.btn_browse_inDB = QPushButton("选择")
        self.btn_browse_inDB.clicked.connect(self.on_select_inDB_clicked)
        db_row.addWidget(lbl_inDB)
        db_row.addWidget(self.txt_inDB, 1)
        db_row.addWidget(self.btn_browse_inDB)

        # --- 第三行：生成命令 + 导出 + 重置 ---
        cmd_row = QHBoxLayout()
        cmd_output_label = QLabel("📝 cmd_output：")
        cmd_output_label.setFixedWidth(90)
        cmd_output_label.setAlignment(Qt.AlignRight | Qt.AlignVCenter)
        self.cmd_output = QLineEdit()
        self.cmd_output.setPlaceholderText("请选择输出文件")
        cmd_output_btn = QPushButton("选择...")
        cmd_output_btn.clicked.connect(self.on_select_cmd_output)
        self.btn_reset = QPushButton("重置")
        self.btn_reset.clicked.connect(self.on_reset_clicked)
        self.btn_generate_cmd = QPushButton("生成命令")
        self.btn_generate_cmd.clicked.connect(self.on_generate_cmd_clicked)
        self.btn_export_cmd = QPushButton("导出命令")
        self.btn_export_cmd.clicked.connect(self.on_export_cmd_clicked)
        self.btn_import_cmd = QPushButton("导入命令")
        self.btn_import_cmd.clicked.connect(self.on_import_cmd_clicked)

        cmd_row.addWidget(cmd_output_label)
        cmd_row.addWidget(self.cmd_output, 1)
        cmd_row.addWidget(cmd_output_btn)
        cmd_row.addStretch(1)
        cmd_row.addWidget(self.btn_generate_cmd)
        cmd_row.addWidget(self.btn_export_cmd)
        cmd_row.addWidget(self.btn_import_cmd)

        # --- 命令文本输出区 ---
        self.txt_cmd_output = QTextEdit()
        self.txt_cmd_output.setPlaceholderText("此处显示生成的命令，每行一条。")
        self.txt_cmd_output.setFixedHeight(150)

        # 添加至操作区
        op_layout.addLayout(top_row)
        op_layout.addLayout(db_row)
        op_layout.addLayout(cmd_row)
        op_layout.addWidget(self.txt_cmd_output)
        main_layout.addWidget(op_group)

        # --- 底部操作 ---
        bottom_layout = QHBoxLayout()  # 一行排列（输入框 + 按钮）

        # count 输入框
        self.count_label = QLabel("线程数：")
        self.count_input = QLineEdit()
        self.count_input.setPlaceholderText("输入线程数量")
        self.count_input.setFixedWidth(80)
        self.count_input.setText("4")  # 默认值
        self.count_input.setValidator(QIntValidator(1, 999))  # 只允许输入数字

        # 执行按钮
        self.run_button = QPushButton("执行多线程入库")
        self.run_button.setFixedHeight(40)
        self.run_button.setStyleSheet(
            "background-color: #0078d7; color: white; font-weight: bold;"
        )
        self.run_button.clicked.connect(self.on_run_parallel)

        # 布局组装
        bottom_layout.addWidget(self.count_label)
        bottom_layout.addWidget(self.count_input)
        bottom_layout.addSpacing(20)
        bottom_layout.addStretch(1)  # 推动右边按钮靠右
        bottom_layout.addWidget(self.run_button)

        # 把底部布局加入主布局
        main_layout.addLayout(bottom_layout)

        # 设置整体大小策略
        self.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Minimum)


    # ================== 文件选择 ==================

    def on_select_products(self):
        files, _ = QFileDialog.getOpenFileNames(
            self, "选择 Product JSON 文件", "", "JSON 文件 (*.json);;所有文件 (*)"
        )
        if files:
            self.product_list.clear()
            for f in files:
                self.product_list.addItem(os.path.basename(f))
            self._product_files = files
        else:
            self._product_files = []

    def on_select_scene(self):
        folder = QFileDialog.getExistingDirectory(self, "选择 Scenes 文件夹")
        if folder:
            self.scene_input.setText(folder)

    def on_select_db(self):
        path, _ = QFileDialog.getOpenFileName(
            self, "选择 DB Config 文件", "", "JSON 文件 (*.json)"
        )
        if path:
            self.db_input.setText(path)

    def on_select_output(self):
        path, _ = QFileDialog.getSaveFileName(
            self, "选择或输入输出日志文件路径", "", "日志文件 (*.txt);;所有文件 (*)"
        )
        if path:
            self.output_input.setText(path)

    def on_select_cmd_output(self):
        path, _ = QFileDialog.getSaveFileName(
            self, "选择或输入输出日志文件路径", "", "日志文件 (*.txt);;所有文件 (*)"
        )
        if path:
            self.cmd_output.setText(path)

    def on_select_inDB_clicked(self):
        file_path, _ = QFileDialog.getOpenFileName(
            self, "选择 inDB 可执行文件", "", "可执行文件 (*.exe)"
        )
        if file_path:
            self.txt_inDB.setText(file_path)

    # ================== 操作逻辑 ==================

    def on_validate_clicked(self):
        """执行 validator 检验"""
        if not self.validator_path:
            QMessageBox.warning(self, "错误", "validator.exe 路径未设置！")
            return

        scenes_dir = self.get_scene_dir()
        product_jsons = self.get_product_jsons()
        if not scenes_dir or not product_jsons:
            QMessageBox.warning(self, "错误", "参数未设置！")
            return

        # 自动命名日志文件
        log_index = 1
        while True:
            log_name = f"validator_log{log_index}.txt"
            log_path = os.path.join(scenes_dir, log_name)
            if not os.path.exists(log_path):
                break
            log_index += 1

        # 显示等待光标
        QApplication.setOverrideCursor(Qt.WaitCursor)

        try:
            with open(log_path, "w") as log_file:
                cmd = (f'{self.validator_path} --scenes_dir "{self.get_scene_dir()}" --product_config ' + " ".join(f'"{p}"' for p in self.get_product_jsons()) )
                subprocess.run(
                    cmd,
                    stdout=log_file,
                    stderr=subprocess.STDOUT,
                    check=True,
                    shell=True
                )

            QMessageBox.information(self, "完成", f"检验完成，日志已保存至 {log_name}")
            self.current_log_path = log_path
            self.btn_open_log.setEnabled(True)

        except Exception as e:
            QMessageBox.critical(self, "错误", f"执行失败：{str(e)}")

        finally:
            # 恢复光标
            QApplication.restoreOverrideCursor()

    def on_open_log_clicked(self):
        if self.current_log_path and os.path.exists(self.current_log_path):
            try:
                os.startfile(self.current_log_path)
            except Exception as e:
                QMessageBox.warning(self, "提示", f"无法打开日志文件：{str(e)}")
        else:
            QMessageBox.warning(self, "提示", "日志文件不存在！")

    def on_generate_cmd_clicked(self):
        inDB_path = self.txt_inDB.text().strip()
        if not inDB_path:
            QMessageBox.warning(self, "提示", "请先设置 inDB 程序路径！")
            return
        cmd = (f'{inDB_path} --scenes_dir "{self.get_scene_dir()}" --product_config ' + " ".join(f'"{p}"' for p in self.get_product_jsons()) + f' --db_config "{self.get_db_config()}" --output_log "{self.get_cmd_output_log_path()}"')

        existing = self.txt_cmd_output.toPlainText().splitlines()
        if cmd in existing:
            QMessageBox.information(self, "提示", "该命令已存在。")
            return
        self.txt_cmd_output.append(cmd)

    def on_export_cmd_clicked(self):
        text = self.txt_cmd_output.toPlainText().strip()
        if not text:
            QMessageBox.warning(self, "提示", "暂无可导出的命令内容。")
            return

        file_path, _ = QFileDialog.getSaveFileName(self, "导出命令文件", "commands.txt", "文本文件 (*.txt)")
        if file_path:
            with open(file_path, "w") as f:
                f.write(text)
            QMessageBox.information(self, "成功", f"命令已导出至：{file_path}")

    def on_import_cmd_clicked(self):
        """导入命令文件"""
        file_path, _ = QFileDialog.getOpenFileName(
            self,
            "导入命令文件",
            "",
            "文本文件 (*.txt);;所有文件 (*)"
        )
        if not file_path:
            return

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                text = f.read().strip()

            if not text:
                QMessageBox.warning(self, "提示", "文件内容为空。")
                return

            self.txt_cmd_output.setPlainText(text)
            QMessageBox.information(self, "成功", f"命令已从 {file_path} 导入。")

        except Exception as e:
            QMessageBox.critical(self, "错误", f"导入失败：{str(e)}")



    def on_reset_clicked(self):
        """重置所有输入"""
        self.product_list.clear()
        self.scene_input.clear()
        self.db_input.clear()
        self.output_input.clear()
        self._product_files = []
        self.btn_open_log.setEnabled(False)
        self.current_log_path = None
        QMessageBox.information(self, "重置", "所有内容已重置。")

    # ================== 对外接口 ==================

    def get_product_jsons(self):
        return self._product_files

    def get_scene_dir(self):
        return self.scene_input.text().strip()

    def get_db_config(self):
        return self.db_input.text().strip()

    def get_output_log_path(self):
        return self.output_input.text().strip()
    
    def get_cmd_output_log_path(self):
        return self.cmd_output.text().strip()

    # ===== 响应函数占位 =====
    def on_run_parallel(self):
        """执行 validator（打开终端或 QProcess）"""
        text = self.txt_cmd_output.toPlainText().strip()
        if not text:
            QMessageBox.warning(self, "提示", "命令内容为空。")
            return

        # 写入临时文件
        tmp_fd, tmp_path = tempfile.mkstemp(suffix=".txt", prefix="validator_cmd_")
        with open(tmp_path, "w", encoding="utf-8") as f:
            f.write(text)
        os.close(tmp_fd)

        # 构造命令
        cmd = [
            self.mparallel_path,
            f"--count={self.count_input.text()}",
            f"--logfile={self.output_input.text()}",
            "--detached",
            f"--input={tmp_path}",
        ]
        cmd_str = f'{self.mparallel_path} --count={self.count_input.text()} --logfile="{self.output_input.text()}" --detached --input="{tmp_path}"'
        print("执行命令:", cmd_str)

        try:
            subprocess.Popen(f"start cmd /K{cmd_str}", shell=True)
            QMessageBox.information(self, "执行中", "已在新终端窗口启动 多线程 任务。")
        except Exception as e:
            QMessageBox.critical(self, "错误", f"执行失败：{str(e)}")

