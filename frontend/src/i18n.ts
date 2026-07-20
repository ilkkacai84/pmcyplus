export type Locale = 'zh-CN' | 'en-US'

const storageKey = 'pm-locale'
export const locale: Locale = localStorage.getItem(storageKey) === 'en-US' ? 'en-US' : 'zh-CN'

const en: Record<string, string> = {
  '中文': 'Chinese', '语言': 'Language',
  '项目管理平台': 'Project Management',
  '企业项目协作空间': 'Enterprise Project Workspace',
  '需求、项目、任务和验收主链路': 'Requirements, projects, tasks and acceptance',
  '统一需求、里程碑、任务与交付验收，让跨部门项目真正透明。': 'Unify requirements, milestones, tasks and acceptance for transparent cross-team delivery.',
  '让每一次协作': 'Make every collaboration',
  '都有清晰的下一步': 'lead to a clear next step',
  '使用企业账号进入项目空间': 'Use your enterprise account to enter the workspace',
  '初始账号由管理员创建，首次启动可使用页面预填的演示管理员。': 'Accounts are created by administrators. The demo administrator is prefilled for first use.',
  '内部与客户需求统一进入需求池': 'Internal and customer requests enter one request pool',
  '所有入口汇总到统一需求池，再进入项目执行。': 'All channels feed one request pool before project execution.',
  '统一查看项目健康度、负责人和计划节点。': 'View project health, owners and planned milestones in one place.',
  '维护部门层级，并由管理员创建内部员工和客户账号。': 'Maintain departments and administrator-created staff and customer accounts.',
  '集中追溯权限、需求、项目、任务和验收等关键操作。': 'Trace key permission, requirement, project, task and acceptance actions.',
  '关键业务操作会自动记录在这里。': 'Critical business actions are recorded here automatically.',
  '需求、任务、审批和验收动态会显示在这里。': 'Requirement, task, approval and acceptance updates appear here.',
  '管理员先核对迁移范围与字段冲突，再执行可追溯的事务性合并。': 'Review migration scope and conflicts before a traceable transactional merge.',
  '合并后来源对象将变为只读，第一版不支持一键撤销。确认继续？': 'Source objects become read-only after merging and cannot be restored in one click. Continue?',
  '任务必须有明确负责人和截止时间': 'Tasks require a clear owner and deadline',
  '完成时填报实际工时并提交验收': 'Log actual hours on completion and submit for acceptance',
  '客户只访问与自己关联的项目数据': 'Customers only access data related to them',
  '保持更新，让团队掌握真实进度': 'Keep updates current so the team sees real progress',
  '项目经理分配任务后将在这里显示。': 'Tasks appear here after assignment by a project manager.',
  '创建带开始和结束时间的任务后生成甘特图。': 'Create scheduled tasks to generate the Gantt chart.',
  '项目执行中的风险和问题会显示在这里。': 'Project risks and issues appear here.',
  '上传文件后会永久保留版本记录。': 'Uploaded files retain a permanent version history.',
  '第一版 MVP': 'Version 1 MVP',
  '工作台': 'Dashboard', '需求中心': 'Requirements', '项目中心': 'Projects',
  '消息通知': 'Notifications', '资源负荷': 'Resource Load', '数据报表': 'Reports',
  '部门视图': 'Department', '账号管理': 'Accounts', '审计日志': 'Audit Log',
  '流程模板': 'Workflow Templates', '合并管理': 'Merge Management', '退出': 'Sign out',
  '欢迎回来': 'Welcome back', '登录': 'Sign in', '正在登录…': 'Signing in…',
  '企业统一身份登录': 'Enterprise SSO', '账号': 'Username', '密码': 'Password',
  '逾期升级周期': 'Overdue escalation', '统一项目视图': 'Unified project view',
  '关键操作留痕': 'Key actions audited', '早上好，': 'Good morning, ',
  '常用操作': 'Quick actions', '协作原则': 'Collaboration principles',
  '这里是需要你关注的项目动态。': 'Here are the project updates that need your attention.',
  '尚未进入项目执行链路': 'Not yet in project execution',
  '跟踪里程碑和任务看板': 'Track milestones and the task board',
  '编号 / 需求': 'ID / Requirement', '分派': 'Assign', '对象类型': 'Object type',
  '请选择目标': 'Select a target', '登录账号': 'Login username', '管理员': 'Administrator',
  '上级': 'Parent', '内部': 'Internal', '暂不指定': 'Not assigned yet',
  '周一至周五默认 8 小时；节假日和个人容量覆盖优先': 'Default 8 hours Monday to Friday; holidays and personal capacity overrides take priority',
  '从项目、部门、成员和客户维度识别进度、负荷、风险与验收效率。': 'Analyze progress, load, risks and acceptance efficiency by project, department, member and customer.',
  '内部项目与临时任务分别维护状态迁移、操作角色、原因和通知事件。': 'Maintain transitions, operator roles, reasons and notification events separately for internal projects and ad-hoc tasks.',
  '内部项目默认流程': 'Default internal project workflow', '临时任务默认流程': 'Default ad-hoc task workflow',
  '我的进行中任务': 'My active tasks', '已逾期任务': 'Overdue tasks', '待分派需求': 'Unassigned requests',
  '新建需求': 'New requirement', '创建需求': 'Create requirement', '需求池为空': 'The request pool is empty',
  '提交第一条需求，开始业务流程。': 'Submit the first requirement to start the workflow.',
  '新建任务': 'New task', '创建任务': 'Create task', '创建项目': 'Create project',
  '创建第一个项目，开始拆分里程碑和任务。': 'Create the first project and break it into milestones and tasks.',
  '还没有项目': 'No projects yet', '查看项目': 'View project', '返回项目': 'Back to project',
  '项目名称': 'Project name', '项目说明': 'Project description', '项目类型': 'Project type',
  '项目经理': 'Project manager', '项目成员': 'Project members', '项目范围': 'Project scope',
  '项目预算': 'Project budget', '维护预算': 'Maintain budget', '预算使用率': 'Budget usage',
  '人工成本': 'Labor cost', '其他费用': 'Other expenses', '预算': 'Budget',
  '内部项目': 'Internal project', '临时任务': 'Ad-hoc task', '全部项目': 'All projects',
  '需求标题': 'Requirement title', '详细说明': 'Details', '来源': 'Source',
  '提交人': 'Requester', '负责人': 'Owner', '参与人': 'Participants', '客户': 'Customer',
  '关联项目': 'Related project', '需求关联项目': 'Related project', '暂不关联项目': 'No project yet',
  '分派需求': 'Assign requirement', '确认分派': 'Confirm assignment', '待分派': 'Unassigned',
  '完善中': 'Refining', '已分配': 'Assigned', '待审批': 'Pending approval',
  '已批准': 'Approved', '已驳回': 'Rejected', '已交付': 'Delivered', '待验收': 'Pending acceptance',
  '已验收': 'Accepted', '已完成': 'Completed', '已取消': 'Cancelled', '已合并': 'Merged',
  '尚未开始': 'Not started', '进行中': 'In progress', '阻塞': 'Blocked', '草稿': 'Draft',
  '提交需求': 'Submit requirement', '提交到需求池': 'Submit to request pool', '推进处理': 'Advance',
  '重新完善': 'Refine again', '提交审批': 'Submit for approval', '批准': 'Approve', '驳回': 'Reject',
  '撤回': 'Withdraw', '需求负责人撤回': 'Owner withdrawal', '创建并关联项目': 'Create and link project',
  '任务名称': 'Task name', '任务看板': 'Task board', '里程碑': 'Milestone', '子任务': 'Subtask',
  '创建里程碑': 'Create milestone', '保存任务': 'Save task', '提交任务交付': 'Submit delivery',
  '变更任务状态': 'Change task status', '解除阻塞': 'Unblock', '提交验收': 'Submit acceptance',
  '确认验收结果': 'Confirm acceptance result', '交付通过': 'Accept delivery', '要求修改': 'Request changes',
  '验收意见': 'Acceptance notes', '可填写验收说明': 'Optional acceptance notes',
  '请说明需要修改的内容': 'Describe the requested changes', '重新提交': 'Resubmit',
  '优先级': 'Priority', '低': 'Low', '中': 'Medium', '高': 'High', '紧急': 'Urgent',
  '状态': 'Status', '类型': 'Type', '描述': 'Description', '说明': 'Notes',
  '预计工时': 'Estimated hours', '实际工时': 'Actual hours', '开始日期': 'Start date',
  '结束日期': 'End date', '截止时间': 'Deadline', '计划开始': 'Planned start', '计划结束': 'Planned end',
  '实际日期时间': 'Actual date/time', '任务完成率': 'Task completion', '任务逾期率': 'Task overdue rate',
  '里程碑达成率': 'Milestone achievement', '客户验收效率': 'Customer acceptance efficiency',
  '成员实际工时': 'Member actual hours', '风险分布': 'Risk distribution',
  '项目文档': 'Project documents', '创建新文档': 'Create document', '更新已有文档': 'Update document',
  '上传文档': 'Upload document', '上传新版本': 'Upload new version', '文档标题': 'Document title',
  '版本说明': 'Version notes', '无版本说明': 'No version notes', '暂无文档': 'No documents',
  '风险与问题': 'Risks and issues', '新增风险': 'Add risk', '保存风险': 'Save risk',
  '风险标题': 'Risk title', '风险等级': 'Risk level', '填写本次风险处理记录': 'Record this risk update',
  '暂无风险': 'No risks', '需要协调': 'Needs coordination', '需要优先处理或调整计划': 'Prioritize or adjust plan',
  '基础甘特图': 'Gantt chart', '计划时间与实际完成状态（只读）': 'Schedule and completion status (read-only)',
  '暂无排期': 'No schedule', '未排期': 'Unscheduled', '存在排期冲突': 'Scheduling conflict',
  '刷新负荷': 'Refresh load', '可用工时': 'Available hours', '标准工时': 'Standard hours',
  '负荷率': 'Load rate', '部门预计工时': 'Department estimated hours', '当前未关闭任务': 'Open tasks',
  '节假日/调休日': 'Holiday / adjusted workday', '工作日期': 'Date', '工作说明': 'Description',
  '设为工作日': 'Set as workday', '保存日历规则': 'Save calendar rule',
  '组织与账号': 'Organization and accounts', '创建部门': 'Create department', '部门名称': 'Department name',
  '上级部门': 'Parent department', '顶级部门': 'Top-level department', '部门负责人': 'Department manager',
  '部门成员': 'Department members', '创建账号': 'Create account', '显示名称': 'Display name',
  '账号类型': 'Account type', '内部用户': 'Internal user', '外部客户': 'External customer',
  '所属部门': 'Department', '角色（可多选）': 'Roles (multiple)', '初始密码': 'Initial password',
  '启用': 'Enable', '停用': 'Disable', '尚未维护部门': 'No departments configured',
  '审计记录': 'Audit records', '操作人': 'Operator', '操作时间': 'Time', '操作': 'Action',
  '业务对象': 'Business object', '变更明细': 'Change details', '暂无审计记录': 'No audit records',
  '通知事件': 'Notification events', '通知事件代码': 'Notification event code',
  '状态迁移': 'State transitions', '允许角色': 'Allowed roles', '规则': 'Rules',
  '必填原因': 'Reason required', '顺序审批步骤': 'Sequential approval steps', '增加步骤': 'Add step',
  '审批记录': 'Approval history', '审批意见': 'Approval comments', '当前步骤': 'Current step',
  '消息': 'Message', '暂无通知': 'No notifications', '标为已读': 'Mark as read',
  '邮件': 'Email', '企业微信': 'WeCom', '移动端': 'Mobile',
  '生成合并预览': 'Generate merge preview', '来源对象（可多选）': 'Source objects (multiple)',
  '目标对象': 'Target object', '字段冲突': 'Field conflicts', '未发现字段冲突。': 'No field conflicts found.',
  '确认并执行合并': 'Confirm and merge', '正在执行': 'Merging', '保留目标：': 'Keep target: ',
  '保存': 'Save', '取消': 'Cancel', '关闭': 'Close', '提交': 'Submit', '确认提交': 'Confirm',
  '应用筛选': 'Apply filters', '刷新': 'Refresh', '处理中…': 'Processing…', '已保存。': 'Saved.',
  '暂无任务': 'No tasks', '暂无说明': 'No description', '暂无项目说明': 'No project description',
  '未分配': 'Unassigned', '未关联': 'Not linked', '未设置': 'Not set', '未设置邮箱': 'No email',
  '待指定': 'To be assigned', '待处理': 'Pending', '等待确认': 'Awaiting confirmation',
  '全部部门': 'All departments', '全部负责人': 'All owners', '全部客户': 'All customers',
  '全部类型': 'All types', '仅本人': 'Mine only', '仅本部门成员和任务': 'Department members and tasks only',
  '文件': 'File', '用户': 'User', '成员': 'Member', '任务': 'Task', '项目': 'Project', '需求': 'Requirement',
  '部门': 'Department', '角色': 'Role', '邮箱': 'Email', '动作': 'Action', '等级': 'Level',
}

function translate(value: string) {
  const trimmed = value.trim()
  if (en[trimmed]) return value.replace(trimmed, en[trimmed])
  let result = value
  for (const [source, target] of Object.entries(en).filter(([source]) => source.length >= 4).sort((a, b) => b[0].length - a[0].length)) {
    result = result.replaceAll(source, target)
  }
  result = result
    .replace(/(\d+)\s*条未读；邮件、(?:企业微信|WeCom)和 Teams 同步进入待投递队列。/g, '$1 unread; email, WeCom and Teams are queued for delivery.')
    .replace(/(\d+)\s*项任务/g, '$1 tasks')
    .replace(/(\d+)\s*个项目/g, '$1 projects')
    .replace(/(\d+)\s*个(?:已验收|Accepted)版本/g, '$1 accepted versions')
  return result
}

function translateNode(node: Node) {
  if (node.nodeType === Node.TEXT_NODE && node.textContent?.trim()) node.textContent = translate(node.textContent)
  if (!(node instanceof Element)) return
  for (const attribute of ['placeholder', 'title', 'aria-label']) {
    const value = node.getAttribute(attribute)
    if (value) node.setAttribute(attribute, translate(value))
  }
  node.childNodes.forEach(translateNode)
}

export function setLocale(next: Locale) {
  localStorage.setItem(storageKey, next)
  window.location.reload()
}

export function installI18n(root: Element) {
  document.documentElement.lang = locale
  if (locale === 'zh-CN') return
  translateNode(root)
  new MutationObserver(records => records.forEach(record => record.addedNodes.forEach(translateNode)))
    .observe(root, { childList: true, subtree: true })
}
