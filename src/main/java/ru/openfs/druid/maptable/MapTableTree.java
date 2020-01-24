package ru.openfs.druid.maptable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class MapTableTree {

	private List<MapTableTreeNode> nodes = new CopyOnWriteArrayList<MapTableTreeNode>();

	public String[] get(String[] testColumns) {
		if ((testColumns == null) || (testColumns.length == 0)) {
			return null;
		}
		for (MapTableTreeNode node : nodes) {
			MapTableTreeNode answer = node.find(testColumns, 0);
			if (answer != null && answer.getValue() != null) {
				return answer.getValue();
			}
		}
		return null;
	}

	public int size() {
		return nodes != null ? nodes.size() : -1;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (MapTableTreeNode e : nodes) {
			sb.append("Node:").append(e).append("\n");
		}
		return sb.toString();
	}

	public boolean put(String[] testerColumns, String[] dataColumns) {
		if (testerColumns.length == 0) {
			return false;
		}

		MapTableTreeNode node;

		Optional<MapTableTreeNode> locate = nodes.stream().filter(n -> ColumnTester.of(testerColumns[0]).equals(n.getTester())).findFirst();
		if (locate.isPresent()) {
			node = locate.get();
		} else {
			node = new MapTableTreeNode(ColumnTester.of(testerColumns[0]));
			this.nodes.add(node);
		}

		for (int i = 1; i < testerColumns.length; i++) {
			Tester nodeTester = ColumnTester.of(testerColumns[i]);
			Optional<MapTableTreeNode> subnode = node.getStream().filter(n -> nodeTester.equals(n.getTester()))
					.findFirst();
			if (subnode.isPresent()) {
				node = subnode.get();
			} else {
				MapTableTreeNode lastNode = new MapTableTreeNode(nodeTester);
				node.addChild(lastNode);
				node = lastNode;
			}
		}

		node.addValue(dataColumns);
		return true;
	}

}
