export interface NotificationResponse {
  notificationId: string;
  notificationType: string;
  title: string;
  message: string;
  relatedDocumentId: string | null;
  relatedVersionId: string | null;
  relatedAnnotationId: string | null;
  relatedCommentId: string | null;
  read: boolean;
  createdAt: string;
}

export interface NotificationSummaryResponse {
  unreadCount: number;
}
