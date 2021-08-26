/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.core.utils.progress.tasks;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.neo4j.gds.core.utils.ProgressLogger;
import org.neo4j.gds.core.utils.progress.EmptyProgressEventTracker;
import org.neo4j.gds.core.utils.progress.ProgressEventTracker;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.Stack;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public class TaskProgressTracker implements ProgressTracker {

    private final Task baseTask;
    private final TaskProgressLogger taskProgressLogger;
    private final ProgressEventTracker eventTracker;
    private final Stack<Task> nestedTasks;
    private Optional<Task> currentTask;

    @TestOnly
    public TaskProgressTracker(
        Task baseTask,
        ProgressLogger progressLogger
    ) {
        this(baseTask, progressLogger, EmptyProgressEventTracker.INSTANCE);
    }

    @TestOnly
    public TaskProgressTracker(TaskProgressTracker progressTracker) {
        this.baseTask = progressTracker.baseTask;
        this.taskProgressLogger = progressTracker.taskProgressLogger;
        this.eventTracker = progressTracker.eventTracker;
        this.currentTask = progressTracker.currentTask;
        this.nestedTasks = progressTracker.nestedTasks;
    }

    public TaskProgressTracker(
        Task baseTask,
        ProgressLogger progressLogger,
        ProgressEventTracker eventTracker
    ) {
        this.baseTask = baseTask;
        this.taskProgressLogger = new TaskProgressLogger(progressLogger, baseTask);
        this.eventTracker = eventTracker;
        this.currentTask = Optional.empty();
        this.nestedTasks = new Stack<>();

        eventTracker.addTaskProgressEvent(baseTask);
    }

    @Override
    public void setEstimatedMaxMemoryInBytes(OptionalLong maxMemory) {
        this.baseTask.setEstimatedMaxMemoryInBytes(maxMemory);
    }

    @Override
    public void beginSubTask() {
        var nextTask = currentTask.map(task -> {
            nestedTasks.add(task);
            return task.nextSubtask();
        }).orElse(baseTask);
        nextTask.start();
        taskProgressLogger.logBeginSubTask(nextTask, parentTask());
        currentTask = Optional.of(nextTask);
    }

    @Override
    public void beginSubTask(String expectedTaskDescription) {
        beginSubTask();
        assertSubTask(expectedTaskDescription);
    }

    @Override
    public void beginSubTask(long taskVolume) {
        beginSubTask();
        setVolume(taskVolume);
    }

    @Override
    public void endSubTask() {
        var currentTask = requireCurrentTask();
        taskProgressLogger.logEndSubTask(currentTask, parentTask());
        currentTask.finish();
        this.currentTask = nestedTasks.isEmpty()
            ? Optional.empty()
            : Optional.of(nestedTasks.pop());
    }

    @Override
    public void endSubTask(String expectedTaskDescription) {
        assertSubTask(expectedTaskDescription);
        endSubTask();
    }

    @Override
    public void logProgress(long value) {
        requireCurrentTask().logProgress(value);
        taskProgressLogger.logProgress(value);
    }

    @Override
    public void setVolume(long volume) {
        requireCurrentTask().setVolume(volume);
        taskProgressLogger.reset(volume);
    }

    @Override
    public ProgressLogger progressLogger() {
        return taskProgressLogger.internalProgressLogger();
    }

    @Override
    public ProgressEventTracker progressEventTracker() {
        return eventTracker;
    }

    @Override
    public void release() {
        eventTracker.release();
    }

    @TestOnly
    public Task currentSubTask() {
        return requireCurrentTask();
    }

    @Nullable
    private Task parentTask() {
        return nestedTasks.isEmpty() ? null : nestedTasks.peek();
    }

    private Task requireCurrentTask() {
        return currentTask.orElseThrow(() -> new IllegalStateException("No more running tasks"));
    }

    private void assertSubTask(String subTaskSubString) {
        if (currentTask.isPresent()) {
            var currentTaskDescription = currentTask.get().description();
            assert currentTaskDescription.contains(subTaskSubString) : formatWithLocale(
                "Expected task name to contain `%s`, but was `%s`",
                subTaskSubString,
                currentTaskDescription
            );
        }
    }
}
