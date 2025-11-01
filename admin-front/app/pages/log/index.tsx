import React, { useEffect, useRef, useState } from "react";
import { ProCard } from "@ant-design/pro-components";
import {
	List,
	Button,
	InputNumber,
	Space,
	Typography,
	Switch,
	Select,
} from "antd";
import { AnsiUp } from "ansi_up";
import DOMPurify from "dompurify";

const { Text } = Typography;

// 默认导出一个可复用的实时日志查看器 React 组件
// 使用方法示例：
// <RealtimeLogViewer urlBase="ws://223.2.43.238:9888/log" initialLines={200} />

export interface LogMessage {
    /** 唯一 ID，用于渲染列表 key */
    id: string;
  
    /** 原始日志文本（含 ANSI 颜色码） */
    raw: string;
  
    /** 转换后的安全 HTML（通过 ansi_up + DOMPurify） */
    textHtml: string;
  
    /** ISO 格式的时间字符串，例如 "2025-11-01T09:00:00.000Z" */
    time: string;
  
    /** 日志级别 */
    level: 'error' | 'warn' | 'info' | 'debug' | string;
  }
  

export default function RealtimeLogViewer({
	urlBase = "ws://223.2.43.238:9888/log",
	initialLines = 200,
	maxBuffer = 2000,
	autoConnect = true,
	showTimestamps = true,
}) {
	const [lines, setLines] = useState(initialLines);
	const [connected, setConnected] = useState(false);
	const [autoscroll, setAutoscroll] = useState(true);
	const [paused, setPaused] = useState(false);
	const [filterLevel, setFilterLevel] = useState("all");
	const wsRef = useRef<WebSocket | null>(null);;
	const listRef = useRef<HTMLElement | null>(null);
	const ansi = useRef(new AnsiUp()).current;

	// messages: 每条 {id, textHtml, raw, time, level?}
	const [messages, setMessages] = useState<LogMessage[]>([]);

	const buildUrl = (base: string, linesNum: number) => {
		// 服务端的接口样例： ws://host:port/log/{lines}
		// 避免重复斜杠
		const suffix = String(linesNum || 0);
		return `${base.replace(/\/+$/, "")}/${suffix}`;
	};

	const connect = (linesNum = lines) => {
		disconnect();
		const url = buildUrl(urlBase, linesNum);
		try {
			const ws = new WebSocket(url);
			wsRef.current = ws;

			ws.onopen = () => {
				setConnected(true);
				// 服务器在连接时会自动返回历史行（根据接口描述），无需额外请求
			};

			ws.onmessage = (ev) => {
				// 假设每个消息是一行或多行文本
				const payload = String(ev.data);
				// 拆分出多行并逐条处理
				const incomingLines = payload.split(/\r?\n/).filter(Boolean);
				const now = new Date();
				const newMsgs = incomingLines.map((ln) => {
					// 可选：简单的日志等级识别（从文本中提取关键词）
					let level = null;
					const lower = ln.toLowerCase();
					if (lower.includes("error") || lower.includes("err"))
						level = "error";
					else if (
						lower.includes("warn") ||
						lower.includes("warning")
					)
						level = "warn";
					else if (lower.includes("info")) level = "info";
					else level = "debug";

					// 将 ANSI 转成 HTML
					const html = ansi.ansi_to_html(ln);
					// sanitize
					const safe = DOMPurify.sanitize(html);

					return {
						id: `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
						raw: ln,
						textHtml: safe,
						time: now.toISOString(),
						level,
					};
				});

				// 如果处于暂停，则不把新消息渲染到列表（但仍可缓冲）
				setMessages((prev) => {
					// 过滤级别
					const filtered = newMsgs.filter(
						(m) => filterLevel === "all" || m.level === filterLevel,
					);
					const merged = paused
						? prev.concat(filtered)
						: prev.concat(filtered);
					// 裁剪为 maxBuffer
					const start = Math.max(0, merged.length - maxBuffer);
					return merged.slice(start);
				});
			};

			ws.onclose = () => {
				setConnected(false);
				wsRef.current = null;
			};

			ws.onerror = (e) => {
				console.error("WebSocket error", e);
				// onclose 会被调用，设置 connected=false
			};
		} catch (err) {
			console.error("connect failed", err);
		}
	};

	const disconnect = () => {
		if (wsRef.current) {
			try {
				wsRef.current.close();
			} catch (e) {
				/* ignore */
			}
			wsRef.current = null;
		}
		setConnected(false);
	};

	// 自动滚动到最新
	useEffect(() => {
		if (!autoscroll || paused) return;
		const el = listRef.current;
		if (el) {
			// use requestAnimationFrame to ensure DOM updated
			requestAnimationFrame(() => { // 下一帧屏幕重绘之前执行一个函数
				el.scrollTop = el.scrollHeight;
			});
		}
	}, [messages, autoscroll, paused]);

	// 自动连接
	useEffect(() => {
		if (autoConnect) connect(lines);
		return () => disconnect();
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	// 控制台操作函数
	const clear = () => setMessages([]);

	// 简单导出文本
	const exportTxt = () => {
		const content = messages.map((m) => `[${m.time}] ${m.raw}`).join("\n");
		const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
		const url = URL.createObjectURL(blob);
		const a = document.createElement("a");
		a.href = url;
		a.download = `logs-${new Date().toISOString()}.txt`;
		a.click();
		URL.revokeObjectURL(url);
	};

	// UI 渲染
	return (
		<ProCard title="实时日志查看" headerBordered collapsible>
			<Space style={{ marginBottom: 8 }} wrap>
				<Text>连接：</Text>
				<Button
					type={connected ? "primary" : "default"}
					onClick={() => (connected ? disconnect() : connect(lines))}
				>
					{connected ? "断开" : "连接"}
				</Button>

				<Text>历史行数：</Text>
				<InputNumber
					min={0}
					max={5000}
					value={lines}
					onChange={(v) => setLines(Number(v || 0))}
				/>
				<Button
					onClick={() => {
						connect(lines);
					}}
				>
					重新连接(含历史)
				</Button>

				<Text>自动滚动</Text>
				<Switch
					checked={autoscroll}
					onChange={(v) => setAutoscroll(v)}
				/>

				<Text>暂停渲染</Text>
				<Switch checked={paused} onChange={(v) => setPaused(v)} />

				<Text>级别过滤</Text>
				<Select
					value={filterLevel}
					style={{ width: 120 }}
					onChange={(v) => setFilterLevel(v)}
				>
					<Select.Option value="all">全部</Select.Option>
					<Select.Option value="error">error</Select.Option>
					<Select.Option value="warn">warn</Select.Option>
					<Select.Option value="info">info</Select.Option>
					<Select.Option value="debug">debug</Select.Option>
				</Select>

				<Button onClick={clear}>清空</Button>
				<Button onClick={exportTxt}>导出</Button>
			</Space>

			<div
				ref={listRef as React.Ref<HTMLDivElement>}
				style={{
					border: "1px solid #f0f0f0",
					borderRadius: 6,
					height: 480,
					overflow: "auto",
					background: "#ffffff",
					padding: 12,
				}}
			>
				<List
					dataSource={messages.filter(
						(m) => filterLevel === "all" || m.level === filterLevel,
					)}
					renderItem={(item) => (
						<List.Item
							key={item.id}
							style={{ padding: "4px 0", border: "none" }}
						>
							<div
								style={{
									width: "100%",
									fontFamily:
										"ui-monospace, SFMono-Regular, Menlo, Monaco, monospace",
									fontSize: 12,
								}}
							>
								{showTimestamps && (
									<Text
										type="secondary"
										style={{ marginRight: 8 }}
									>
										[{new Date(item.time).toLocaleString()}]
									</Text>
								)}
								<span
									// 使用 dangerouslySetInnerHTML：已使用 DOMPurify 做 sanitize
									dangerouslySetInnerHTML={{
										__html: item.textHtml,
									}}
								/>
							</div>
						</List.Item>
					)}
				/>
			</div>
		</ProCard>
	);
}
