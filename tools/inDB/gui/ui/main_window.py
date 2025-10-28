# ui/main_window.py
import os
from PyQt5.QtWidgets import (
    QWidget, QVBoxLayout, QLabel, QMessageBox, QPushButton, QFrame
)
from PyQt5.QtCore import Qt

from ui.widgets.main_widget import MainWidget


class MainWindow(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("ARD入库工具")
        self.resize(900, 700)

        self.validator_path = None
        self.mparallel_path = None
        self.init_ui()
        self.check_validator()

    def init_ui(self):
        layout = QVBoxLayout(self)
        layout.setSpacing(12)
        layout.setContentsMargins(20, 20, 20, 20)

        # --- 顶部状态 ---
        self.status_label = QLabel("Validator 状态：未检测")
        self.status_label.setStyleSheet("font-weight: bold; color: gray;")
        layout.addWidget(self.status_label)

        # --- 各功能区 ---
        # self.product_widget = ProductConfigWidget()
        # self.scene_widget = SceneConfigWidget()
        # self.db_output_widget = DbOutputWidget()
        # self.command_panel = CommandPanelWidget()
        self.main_widget = MainWidget()

        layout.addWidget(self.main_widget)

        # 分割线
        line1 = QFrame()
        line1.setFrameShape(QFrame.HLine)
        line1.setFrameShadow(QFrame.Sunken)
        layout.addWidget(line1)

    def check_validator(self):
        exe_path1 = os.path.join(os.getcwd(), "validator.exe")
        exe_path2 = os.path.join(os.getcwd(), "mparallel.exe")
        if os.path.exists(exe_path1) and os.path.exists(exe_path2):
            self.validator_path = exe_path1
            self.mparallel_path = exe_path2
            self.status_label.setVisible(False)
        else:
            QMessageBox.critical(self, "错误", "未找到相关exe，请将其放在程序同目录下！")
            self.status_label.setText("❌ 未找到exe")
            self.status_label.setStyleSheet("color: red; font-weight: bold;")

    # ===== 响应函数占位 =====
    def on_run_validator(self):
        """执行 validator（打开终端或QProcess）"""
        pass
