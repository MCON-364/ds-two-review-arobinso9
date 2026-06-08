package edu.touro.mcon364.finalreview.orderflowhandoff.exercises;

import edu.touro.mcon364.finalreview.model.Action;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * In-class Exercise 1 — Action History
 *
 * A simple editor needs to remember actions so the user can undo and redo work.
 *
 * Requirements:
 * - perform(action) records a newly completed action.
 * - undo() removes and returns the action that should be undone next.
 * - redo() removes and returns the action that should be redone next.
 * - undo() returns Optional.empty() when there is nothing available to undo.
 * - redo() returns Optional.empty() when there is nothing available to redo.
 * - performing a new action after one or more undo operations makes the old redo path invalid.
 * - getUndoCount() returns how many actions are currently available to undo.
 * - getRedoCount() returns how many actions are currently available to redo.
 *
 * You may add private fields and private helper methods.
 * Do not change the public method signatures.
 * Before coding, decide:
 * - What information does this class need to remember? whats pushed and popped
 * - What is the appropriate data structure - ArrayDeque
 * - Which operation should be fastest? popping so LIFO- use a stack
 * - When an action is undone, where should it go so it can be redone later? on the redo
 * - What should happen to redo history after a brand-new action is performed?

 */
public class ActionHistory {

    ArrayDeque<Action> undo = new ArrayDeque<>();
    ArrayDeque<Action> redo = new ArrayDeque<>();

    public void perform(Action action) {
        // TODO: implement based on the requirements above
        undo.push(action); // when we do something then we want to be able to undo it later
       // when we perform an action we clear the redo stack
        redo.clear();
    }

    public Optional<Action> undo() {
        // TODO: implement based on the requirements above
        // when u type something- it gets added to the undo stack in case u want to undo it later
        // then u push it onto the redo stack in case u want to redo the action u just undid
        if (undo.isEmpty()) {
            return Optional.empty(); // return empty instead of crashing
        }


        Action action = undo.pop();
        redo.push(action); // Move it to redo so the user can bring it back

        return Optional.ofNullable(action);
    }

    public Optional<Action> redo() {
        // TODO: implement based on the requirements above
        // if we want to redo something we deleted we pop from the redo stack and then since
        // we just typed that- we push onto the undo stack again - in case we change our mind again :)
        if (redo.isEmpty()) {
            return Optional.empty(); // return empty if nothing to redo
        }

        Action action = redo.pop();
        undo.push(action); // Move it back to undo so the user can undo it again

        return Optional.ofNullable(action);
    }

    public int getUndoCount() {
        // TODO: implement based on the requirements above
        return undo.size();
    }

    public int getRedoCount() {
        // TODO: implement based on the requirements above
        return redo.size();
    }
}
