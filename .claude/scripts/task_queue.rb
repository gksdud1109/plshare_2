#!/usr/bin/env ruby
# frozen_string_literal: true

require "yaml"
require "time"

queue_file = ARGV.shift || ENV["QUEUE_FILE"] || "docs/10-operations/tasks/task-queue.yaml"
command = ARGV.shift

abort("queue file not found: #{queue_file}") unless File.exist?(queue_file)
abort("command required") unless command

data = YAML.load_file(queue_file)
data["tasks"] ||= []

def task_by_id(data, task_id)
  data["tasks"].find { |task| task["id"] == task_id }
end

def deps_done?(data, task)
  deps = task["depends_on"] || []
  deps.all? do |dep_id|
    dep = task_by_id(data, dep_id)
    dep && dep["status"] == "done"
  end
end

def save!(queue_file, data)
  File.write(queue_file, Psych.dump(data, indentation: 2, line_width: -1))
end

case command
when "ids-by-status"
  status = ARGV.fetch(0)
  ids = data["tasks"].select { |task| task["status"] == status }.map { |task| task["id"] }
  puts(ids.join("\n"))
when "field"
  task_id = ARGV.fetch(0)
  field = ARGV.fetch(1)
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  value = task[field]
  if value.is_a?(Array)
    puts(value.join("\n"))
  else
    puts(value.nil? ? "null" : value)
  end
when "next-ready-pending"
  task = data["tasks"].find { |item| item["status"] == "pending" && deps_done?(data, item) }
  puts(task ? task["id"] : "")
when "next-pending-for-agent"
  agent = ARGV.fetch(0)
  task = data["tasks"].find { |item| item["status"] == "pending" && item["agent"] == agent && deps_done?(data, item) }
  puts(task ? task["id"] : "")
when "mark-in-progress"
  task_id = ARGV.fetch(0)
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  task["status"] = "in_progress"
  task["started_at"] = Time.now.iso8601
  task["attempt_count"] = (task["attempt_count"] || 0) + 1
  task["last_error"] = nil
  save!(queue_file, data)
when "mark-finished"
  task_id = ARGV.fetch(0)
  status = ARGV.fetch(1)
  exit_code = ARGV.fetch(2).to_i
  last_error = ARGV[3]
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  task["status"] = status
  task["finished_at"] = Time.now.iso8601
  task["exit_code"] = exit_code
  task["last_error"] = last_error unless last_error.nil? || last_error.empty?
  save!(queue_file, data)
when "set-status"
  task_id = ARGV.fetch(0)
  status = ARGV.fetch(1)
  last_error = ARGV[2]
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  task["status"] = status
  task["last_error"] = last_error unless last_error.nil? || last_error.empty?
  task["last_error"] = nil if last_error == "__NULL__"
  save!(queue_file, data)
when "unblock-ready"
  updated = []
  data["tasks"].each do |task|
    next unless task["status"] == "blocked"
    next unless deps_done?(data, task)

    # 승인 게이트는 명시적으로 true일 때만 awaiting_approval, 기본은 바로 pending
    task["status"] = task["requires_approval"] == true ? "awaiting_approval" : "pending"
    task["last_error"] = nil
    updated << task["id"]
  end
  save!(queue_file, data)
  puts(updated.join("\n"))
when "deps-done"
  task_id = ARGV.fetch(0)
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  puts(deps_done?(data, task) ? "true" : "false")
when "approve"
  task_id = ARGV.fetch(0)
  task = task_by_id(data, task_id)
  abort("task not found: #{task_id}") unless task
  abort("task is not awaiting_approval: #{task_id}") unless task["status"] == "awaiting_approval"
  task["status"] = "pending"
  task["last_error"] = nil
  save!(queue_file, data)
  puts("approved: #{task_id}")
when "approve-all"
  approved = []
  data["tasks"].each do |task|
    next unless task["status"] == "awaiting_approval"
    task["status"] = "pending"
    task["last_error"] = nil
    approved << task["id"]
  end
  save!(queue_file, data)
  puts(approved.join("\n"))
else
  abort("unsupported command: #{command}")
end
