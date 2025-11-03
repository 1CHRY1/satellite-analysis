package com.ogms.dge.workspace.modules.workflow.dto;

/**
 * @name: DAGDto
 * @description: 有向无环图
 * @author: Lingkai Shi
 * @date: 12/18/2024 10:46 AM
 * @version: 1.0
 */
import java.util.*;

public class DAGDto<T> {
    private Map<String, T> nodes = new HashMap<>(); // 节点：UUID -> 节点内容
    private Map<String, List<String>> edges = new HashMap<>(); // 边：每个节点所依赖的节点集合
    private Set<String> completedTasks = new HashSet<>(); // 记录已完成的任务

    // 添加节点
    public void addNode(String uuid, T node) {
        nodes.put(uuid, node);
        edges.putIfAbsent(uuid, new ArrayList<>());
    }

    // 添加边（依赖关系）
    public void addEdge(String fromUuid, String toUuid) {
        if (!nodes.containsKey(fromUuid) || !nodes.containsKey(toUuid)) {
            throw new IllegalArgumentException("Both nodes must be added before creating an edge.");
        }
        edges.get(fromUuid).add(toUuid);
    }

    // 获取节点
    public T getNode(String uuid) {
        return nodes.get(uuid);
    }

    // 获取所有没有依赖的初始节点
    public List<T> getInitialNodes() {
        List<T> initialNodes = new ArrayList<>();
        Set<String> dependentNodes = new HashSet<>();
        for (List<String> dependencies : edges.values()) {
            dependentNodes.addAll(dependencies);
        }

        for (String uuid : nodes.keySet()) {
            if (!dependentNodes.contains(uuid)) {
                initialNodes.add(nodes.get(uuid));
            }
        }
        return initialNodes;
    }

    // 获取节点的后续节点（依赖此节点的其他任务）广度优先遍历
    public List<T> getNextNodes(String uuid) {
        List<T> nextNodes = new ArrayList<>();
        for (String targetUuid : edges.keySet()) {
            if (edges.get(uuid).contains(targetUuid)) {
                nextNodes.add(nodes.get(targetUuid));
            }
        }
        return nextNodes;
    }

    // 检查某个任务是否可以执行（即它的所有依赖是否已经执行完成）
    public boolean canExecute(String uuid) {
        List<String> dependencies = new ArrayList<>();
        for (String key : edges.keySet()) {
            if (edges.get(key).contains(uuid)) {
                dependencies.add(key);
            }
        }
        if (dependencies.isEmpty()) {
            return true; // 没有依赖，直接可以执行
        }

        for (String depUuid : dependencies) {
            // 如果有一个依赖未完成，则不能执行
            if (!completedTasks.contains(depUuid)) {
                return false;
            }
        }
        return true;
    }

    public void handleCompleteTask(String taskUuid) {
        completedTasks.add(taskUuid); // 标记任务完成
    }

    public List<String> generateTopologicalOrder() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String node : nodes.keySet()) {
            inDegree.put(node, 0); // 初始化入度为 0
        }

        // 计算每个节点的入度
        for (List<String> dependencies : edges.values()) {
            for (String dep : dependencies) {
                inDegree.put(dep, inDegree.get(dep) + 1);
            }
        }

        // 拓扑排序算法
        Queue<String> queue = new LinkedList<>();
        List<String> topologicalOrder = new ArrayList<>();

        // 入度为 0 的节点入队
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        // BFS 进行拓扑排序
        while (!queue.isEmpty()) {
            String current = queue.poll();
            topologicalOrder.add(current);

            // 减少后续节点的入度
            for (String neighbor : edges.getOrDefault(current, new ArrayList<>())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.add(neighbor);
                }
            }
        }

        // 检查是否有环
        if (topologicalOrder.size() != nodes.size()) {
            throw new IllegalStateException("DAG has cycles, topological order cannot be determined.");
        }

        return topologicalOrder;
    }

    // 检查是否存在环（基于拓扑排序的检测）
    public boolean hasCycle() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String uuid : nodes.keySet()) {
            inDegree.put(uuid, 0);
        }
        for (List<String> dependencies : edges.values()) {
            for (String depUuid : dependencies) {
                inDegree.put(depUuid, inDegree.get(depUuid) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        int visitedCount = 0;
        while (!queue.isEmpty()) {
            String current = queue.poll();
            visitedCount++;
            for (String next : edges.getOrDefault(current, new ArrayList<>())) {
                inDegree.put(next, inDegree.get(next) - 1);
                if (inDegree.get(next) == 0) {
                    queue.add(next);
                }
            }
        }

        return visitedCount != nodes.size();
    }

}

