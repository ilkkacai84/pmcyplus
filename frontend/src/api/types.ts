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

export interface WorkflowTransition {
  id: number
  objectType: 'TASK' | 'REQUIREMENT'
  fromStatus: string
  toStatus: string
  allowedRoles: Role[]
  enabled: boolean
  requiresReason: boolean
  notificationEvent?: string
}

export interface WorkflowTemplate {
  id: number
  name: string
  projectType: 'INTERNAL' | 'TEMPORARY'
  active: boolean
  transitions: WorkflowTransition[]
  approvalSteps: WorkflowApprovalStep[]
}

export interface WorkflowApprovalStep {
  id: number
  stepOrder: number
  name: string
  approverRole: Role
  active: boolean
}

export interface ApprovalAction {
  id: number
  stepOrder: number
  actorName: string
  decision: 'APPROVED' | 'REJECTED' | 'WITHDRAWN'
  opinion?: string
  createdAt: string
}

export interface ApprovalInstance {
  id: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'
  currentStep: number
  currentStepName?: string
  submitterName: string
  createdAt: string
  completedAt?: string
  actions: ApprovalAction[]
}

export interface NotificationItem {
  id: number
  status: 'SENT' | 'READ'
  eventType: string
  title: string
  content?: string
  objectType?: string
  objectId?: number
  escalationLevel: number
  createdAt: string
}

export interface NotificationInbox { unread: number; items: NotificationItem[] }

export interface RiskUpdate { id: number; authorName: string; note: string; createdAt: string }
export interface ProjectRisk {
  id: number; title: string; description?: string; riskLevel: 'LOW'|'MEDIUM'|'HIGH'|'CRITICAL'
  status: 'OPEN'|'MITIGATING'|'RESOLVED'|'CLOSED'; ownerId: number; ownerName: string
  milestoneId?: number; taskId?: number; createdAt: string; updates: RiskUpdate[]
}
export interface DocumentVersion { id:number; versionNo:number; fileName:string; contentType?:string; fileSize:number; note?:string; uploadedBy:string; uploadedAt:string }
export interface ProjectDocument { id:number; title:string; customerVisible:boolean; createdBy:string; createdAt:string; versions:DocumentVersion[] }
export interface MemberLoad { userId:number; userName:string; departmentName?:string; capacityHours:number; allocatedHours:number; actualHours:number; loadRate:number; conflict:boolean }
export interface ResourceReport { from:string; to:string; calculationRule:string; members:MemberLoad[] }
export interface ReportSummary { projectCount:number; taskCount:number; completionRate:number; overdueRate:number; milestoneAchievementRate:number; departmentLoad:Record<string,number>; memberHours:Record<string,number>; riskDistribution:Record<string,number>; reviewedDeliveries:number; averageAcceptanceHours:number }

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
  projectType: 'INTERNAL' | 'TEMPORARY'
  status: RequirementStatus
  priority: Priority
  createdAt: string
}
