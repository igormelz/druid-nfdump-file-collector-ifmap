package ru.openfs.druid.maptable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


public final class MapTableTreeNode {
    private List<MapTableTreeNode> nodes = new ArrayList<MapTableTreeNode>();
    private String[] data;
    private final Tester tester;
    public MapTableTreeNode parent;

    public MapTableTreeNode(Tester tester) {
        this.tester = tester;
    }

    public Tester getTester() {
        return this.tester;
    }

    /**
     * check tester condition on dataToTest
     * @param dataToTest
     * @return
     */
    public boolean test(String dataToTest) {
        return this.tester.test(dataToTest);
    }
    
    public void addValue(String[] data) {
        this.data = data;
    }

    public String[] getValue() {
        return data;
    }

    /**
     * add children node
     * @param node
     */
    public void addChild(MapTableTreeNode node) {
        this.nodes.add(node);
        node.parent = this;
    }

    public Stream<MapTableTreeNode> getStream() {
    	return this.nodes.stream();
    }
    
    /**
     * return last child 
     * @return
     */
    public MapTableTreeNode getLastChild() {
        return (this.nodes.isEmpty()) ? null : this.nodes.get(this.nodes.size() - 1);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" tester=").append(tester);
        nodes.stream().forEach(e -> {
            sb.append("\n").append(e);
        });
        if (getValue() != null) {
            sb.append("\nDATA=");
            for (String d : getValue()) {
                sb.append(d).append(",");
            }
        }
        return sb.toString();
    }

    public MapTableTreeNode find(String[] testColumns, int index) {
        if (!test(testColumns[index])) {
            return null;
        }
        
        // if this last column  
        if (index == testColumns.length - 1) {
            return this;
        }
        
        // test children nodes for next column
        for (MapTableTreeNode node : nodes) {
            MapTableTreeNode answer = node.find(testColumns, index+1);
            if (answer != null) {
                return answer;
            }
        }

        return null;        
    }
}
