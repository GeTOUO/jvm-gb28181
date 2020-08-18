package com.getouo.gb.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class TreeBuilder {
    static class TreeNode {

        public TreeNode(String id, String parent, Set<TreeNode> chi) {
            this.id = id;
            this.parent = parent;
            this.chi = chi;
        }

        String id;
        String parent;
        Set<TreeNode> chi = new HashSet<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreeNode treeNode = (TreeNode) o;
            return Objects.equals(id, treeNode.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "TreeNode{" +
                    "id:'" + id + '\'' +
                    ", parent:'" + parent + '\'' +
                    ", chi:[" + chi.stream().map(TreeNode::toString).reduce((a, b) -> a + "," + b).orElse("") + "]" +
                    '}';
        }
    }

    /**
     * @param node    从根开始的树
     * @param nodeIds 用户需要的节点
     * @return
     */
    public static Optional<TreeNode> selectFilter(TreeNode node, Set<String> nodeIds) {
        boolean contains = nodeIds.contains(node.id);
        Set<TreeNode> collect = node.chi.stream().map(cn -> selectFilter(cn, nodeIds)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
        if (contains) return Optional.of(node);
        else if (!collect.isEmpty()) {
            node.chi = collect;
            return Optional.of(node);
        } else {
            return Optional.empty();
        }
    }

    // 将所有 list节点 变成树
    public static TreeNode toTree(List<TreeNode> source) {
        AtomicReference<TreeNode> rootContainer = new AtomicReference<>();
        final Map<String, TreeNode> pBuf = new HashMap<>();
        source.forEach(s -> pBuf.put(s.id, s));
        source.forEach(t -> {
            if (pBuf.containsKey(t.parent)) {
                pBuf.get(t.parent).chi.add(t);
            } else {
                if (rootContainer.get() == null) {
                    rootContainer.set(t);
                } else {
                    throw new IllegalArgumentException("多个根");
                }
            }
        });
        TreeNode root = rootContainer.get();
        if (root == null) throw new IllegalArgumentException("无节点");
        return root;
    }

    static TreeNode p(String id, String parent) {
        return new TreeNode(id, parent, new HashSet<>());
    }

    public static void main(String[] args) {

        List<TreeNode> treeNodes = Arrays.asList(
                p("root", "un"),
                p("L1", "root"), p("L2", "root"), p("L3", "root"),
                p("LL1", "L1"), p("LL2", "L2"), p("LL3", "L2")
        );

        TreeNode treeNode = toTree(treeNodes);
        System.err.println(treeNode);

        Optional<TreeNode> ll1 = selectFilter(treeNode, new HashSet<String>(Arrays.asList("LL1", "LL3")));
        System.err.println(ll1);

    }


}
