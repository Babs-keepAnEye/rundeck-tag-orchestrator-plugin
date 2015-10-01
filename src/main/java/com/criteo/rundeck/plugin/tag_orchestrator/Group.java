package com.criteo.rundeck.plugin.tag_orchestrator;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.*;

public class Group {
    public final String name;
    final Stack<INodeEntry> todoNodes = new Stack<INodeEntry>();
    final Map<String, INodeEntry> inProgressNodes = new HashMap<String, INodeEntry>();
    public final int size;
    private final double maxPerGroup;
    private final boolean stopProcessingGroupAfterTooManyFailure;

    private int failed = 0;

    public int failed_count() {
      return failed;
    }

    public int processing_count() {
      return inProgressNodes.size();
    }

    public int todo_count() {
      return todoNodes.size();
    }


    public Group(String name, double maxPerGroup, boolean stopProcessingGroupAfterTooManyFailure, Collection<INodeEntry> todoNodes) {
        this.name = name;
        this.todoNodes.addAll(todoNodes);
        this.stopProcessingGroupAfterTooManyFailure = stopProcessingGroupAfterTooManyFailure;
        this.maxPerGroup = maxPerGroup;
        this.size = todoNodes.size();
        System.out.println(String.format("%s group contains %d nodes", name, size));
    }

    // return true if nodeCount is larger than ~maxPerGroup
    private boolean tooMany(int nodeCount) {
        if (maxPerGroup < 1) { // it is a percentage of nodes
            return nodeCount * 100 / size >= maxPerGroup * 100;
        } else { // it is a hard number of nodes
            return nodeCount >= maxPerGroup;
        }
    }

    public boolean failed() {
        System.out.println(name + ", failed: " + failed);
        System.out.println(name + ", is complete: " + tooMany(failed));

        return tooMany(failed);
    }

    public boolean isComplete() {
        return todoNodes.isEmpty() || (stopProcessingGroupAfterTooManyFailure && failed());
    }

    public boolean canProcessANode() {
        return !tooMany(failed + inProgressNodes.size());
    }

    public INodeEntry nextNode() {
        if (!isComplete() && canProcessANode()) {
            INodeEntry node = todoNodes.pop();
            System.out.println("Process " + node.extractHostname() + " from " + name);
            inProgressNodes.put(node.extractHostname(), node);
            return node;
        } else {
            return null;
        }
    }

    public void returnNode(INodeEntry node, boolean success, NodeStepResult nodeStepResult) {
        System.out.println(String.format("Returning %s in %s (success: %b)", node.extractHostname(), name, success));
        if (!success) {
            failed++;
        }
        INodeEntry e = inProgressNodes.remove(node.extractHostname());
        if (e == null) {
            System.err.println(String.format("%s was not in progress but has just been returned. It should be impossible", node.extractHostname()));
        }
    }
}

