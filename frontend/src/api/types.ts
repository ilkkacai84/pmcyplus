export type Role = 'ADMIN' | 'PROJECT_MANAGER' | 'MEMBER' | 'DEPARTMENT_MANAGER' | 'CUSTOMER'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type ProjectStatus = 'DRAFT' | 'ACTIVE' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED' | 'MERGED'
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'PENDING_ACCEPTANCE' | 'COMPLETED' | 'CANCELLED' | 'MERGED'
export type DeliveryStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CHANGES_REQUESTED'
export type RequirementStatus = 'UNASSIGNED' | 'REFINING' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'DELIVERED' | 'ACCEPTED' | 'MERGED'

export interface CurrentUser {
  id: number
  username: string
  displayName: string
  userType: 'INTERNAL' | 'CUSTOMER'
  roles: Role[]
}

export interface UserSummary {
  id: number
  username: string
  displayName: string
  email?: string
  userType: 'INTERNAL' | 'CUSTOMER'
  roles: Role[]
  departmentId?: number
  departmentName?: string
}

export interface DepartmentSummary {
  id: number
  name: string
  parentId?: number
  managerId?: number
  managerName?: string
}

export interface DepartmentTask {
  id: number
  projectId: number
  projectName: string
  title: string
  ownerId: number
  ownerName: string
  status: TaskStatus
  plannedEndAt?: string
}

export interface DepartmentScope {
  department?: DepartmentSummary
  tasks: DepartmentTask[]
}

export interface AuditLog {
  id: number
  actorId?: number
  actorName: string
  action: string
  objectType: string
  objectId?: number
  detailJson?: string
  createdAt: string
}

export interface ProjectSummary {
  id: number
  code: string
  name: string
  projectType: 'INTERNAL' | 'TEMPORARY'
  status: ProjectStatus
  priority: Priority
  managerId: number
  managerName: string
  plannedEndAt?: string
}

export interface Milestone {
  id: number
  name: string
  ownerId?: number
  ownerName?: string
  plannedAt?: string
  status: ProjectStatus
}

export interface Task {
  id: number
  milestoneId?: number
  parentTaskId?: number
  title: string
  description?: string
  ownerId: number
  ownerName: string
  status: TaskStatus
  priority: Priority
  plannedStartAt?: string
  plannedEndAt?: string
  estimatedHours: number
  actualHours: number
}

export interface ProjectDetails {
  project: ProjectSummary
  description?: string
  budget: number
  laborCost: number
  otherCost: number
  milestones: Milestone[]
  tasks: Task[]
  deliveries: DeliveryVersion[]
}

export interface DeliveryVersion {
  id: number
  taskId: number
  versionNo: number
  submittedBy: number
  submitterName: string
  submissionNote?: string
  status: DeliveryStatus
  reviewedBy?: number
  reviewerName?: string
  reviewOpinion?: string
  submittedAt: string
  reviewedAt?: string
}

export interface Requirement {
  id: number
  requirementNo: string
  source: 'WEB' | 'EMAIL' | 'WECHAT' | 'MOBILE'
  title: string
  description?: string
  submitterName: string
  customerId?: number
  assigneeId?: number
  assigneeName?: string
  projectId?: number
  status: RequirementStatus
  priority: Priority
  createdAt: string
}
