package org.fengling.gugutask.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.fengling.gugutask.dto.TaskDetailsD;
import org.fengling.gugutask.pojo.Task;
import org.fengling.gugutask.pojo.TaskTag;
import org.fengling.gugutask.security.jwt.JwtUtil;
import org.fengling.gugutask.service.TagService;
import org.fengling.gugutask.service.TaskService;
import org.fengling.gugutask.service.TaskTagService;
import org.fengling.gugutask.service.TaskTypeService;
import org.fengling.gugutask.util.R;
import org.fengling.gugutask.util.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskTagService taskTagService;
    private final TaskTypeService taskTypeService;
    private final JwtUtil jwtUtil;
    private final TagService tagService;
    @Autowired
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    public TaskController(TaskService taskService, TaskTagService taskTagService, TaskTypeService taskTypeService, JwtUtil jwtUtil, TagService tagService) {
        this.taskService = taskService;
        this.taskTagService = taskTagService;
        this.taskTypeService = taskTypeService;
        this.jwtUtil = jwtUtil;
        this.tagService = tagService;
    }

    // 根据用户ID查询任务及其标签和类型
    @GetMapping("/user")
    public R<List<TaskDetailsD>> getTaskByUserId(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 提取JWT token
        Long userId = jwtUtil.extractUserId(token);  // 从JWT中提取userId

        // 获取封装好的 TaskDetailsD
        List<TaskDetailsD> taskDetailsDList = taskService.getTasksWithDetailsByUserId(userId);

        return R.success(taskDetailsDList);
    }

    // 根据任务类型查询任务（附带分页）
    @GetMapping("/user/tasktype")
    public R<Page<TaskDetailsD>> getTasksByType(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "taskTypeId") Long taskTypeId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "5") Integer size) {
        String token = authHeader.substring(7);  // 提取JWT token
        Long userId = jwtUtil.extractUserId(token);  // 从JWT中提取userId

        if (taskTypeId == null) {
            return R.error("咱没有拿到你要的任务类型");
        }
        // 分页查询
        Page<TaskDetailsD> taskDetailsPage = taskService.getTasksWithDetailsByUserIdAndTaskType(userId, taskTypeId, page, size);
        return R.success(taskDetailsPage);

    }


    // 创建任务
    @PostMapping
    public R<String> createTask(@RequestBody Task task, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 提取JWT token
        Long userId = jwtUtil.extractUserId(token);  // 从JWT中提取userId
        task.setUserId(userId);  // 设置任务的userId
        task.setId(snowflakeIdGenerator.generateId());
        taskService.save(task);
        return R.success("任务创建成功");
    }

    // 更新任务
    @PutMapping("/{id}")
    public R<String> updateTask(@PathVariable Long id, @RequestBody Task task, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 提取JWT token
        Long userId = jwtUtil.extractUserId(token);  // 从JWT中提取userId

        // 获取现有任务
        Task existingTask = taskService.getById(id);
        if (existingTask != null && existingTask.getUserId().equals(userId)) {
            // 确保任务的userId不被更改
            task.setId(id);
            task.setUserId(userId);

            // 检查并保留现有字段
            if (task.getName() == null || task.getName().isEmpty()) task.setName(existingTask.getName());
            if (task.getDetail() == null || task.getDetail().isEmpty()) task.setDetail(existingTask.getDetail());
            if (task.getPriority() == null || task.getPriority().isEmpty())
                task.setPriority(existingTask.getPriority());
            if (task.getDate1() == null) task.setDate1(existingTask.getDate1());
            if (task.getDate2() == null) task.setDate2(existingTask.getDate2());
            if (task.getStatus() == null || task.getStatus().isEmpty()) task.setStatus(existingTask.getStatus());
            if (task.getTypeId() == null) task.setTypeId(existingTask.getTypeId());

            taskService.updateById(task);
            return R.success("更新~成功！");
        }
        return R.forbidden("无权限更新此任务");
    }


    // 删除任务
    @DeleteMapping("/{id}")
    public R<String> deleteTask(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // 提取JWT token
        Long userId = jwtUtil.extractUserId(token);  // 从JWT中提取userId

        // 1. 获取要删除的任务
        Task existingTask = taskService.getById(id);
        if (existingTask != null && existingTask.getUserId().equals(userId)) {

            // 2. 删除任务的所有关联标签 (task_tags表中的记录)
            taskTagService.remove(new QueryWrapper<TaskTag>().eq("task_id", id));

            // 3. 删除任务本身 (tasks表中的记录)
            taskService.removeById(id);

            return R.success("任务及其关联的标签删除成功！");
        }

        return R.forbidden("无权限删除此任务");
    }

}
