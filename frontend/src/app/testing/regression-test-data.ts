import { signal } from '@angular/core';
import { of } from 'rxjs';

import { AnnotationResponse } from '../core/models/annotation.model';
import { CurrentUserResponse, UserResponse, UserRole } from '../core/models/auth.model';
import { DocumentDetailsResponse, DocumentListResponse, DocumentUploadResponse } from '../core/models/document.model';
import { NotificationResponse } from '../core/models/notification.model';

export const normalUser = user('normal-1', 'normaluser', 'NORMAL_USER');
export const staffUser = user('staff-1', 'staffuser', 'STAFF');
export const supervisorUser = user('supervisor-1', 'supervisor', 'SUPERVISOR');

export function user(userId: string, username: string, role: UserRole): UserResponse & CurrentUserResponse {
  return {
    userId,
    username,
    email: `${username}@docuvra.local`,
    mobile: `+1000000${userId.replace(/\D/g, '').padStart(3, '0')}`,
    role,
    active: true,
    forcePasswordChange: false,
    loginEnabled: true,
    createdAt: '2026-06-01T10:00:00',
    lastLoginAt: null
  };
}

export const documentList: DocumentListResponse[] = [
  {
    documentId: 'doc-1',
    title: 'Policy PDF',
    latestVersionNumber: 2,
    latestVersionId: 'ver-2',
    thumbnailUrl: '/api/documents/doc-1/versions/ver-2/thumbnail',
    uploadedByUserId: supervisorUser.userId,
    uploadedByName: supervisorUser.username,
    createdAt: '2026-06-01T10:00:00',
    updatedAt: '2026-06-01T11:00:00'
  }
];

export const documentDetails: DocumentDetailsResponse = {
  documentId: 'doc-1',
  title: 'Policy PDF',
  latestVersionNumber: 2,
  uploadedByUserId: supervisorUser.userId,
  uploadedByName: supervisorUser.username,
  assignments: [],
  versions: [
    {
      versionId: 'ver-1',
      versionNumber: 1,
      fileName: 'policy.pdf',
      mimeType: 'application/pdf',
      fileSize: 1024,
      pageCount: 1,
      status: 'READY',
      thumbnailUrl: '/api/documents/doc-1/versions/ver-1/thumbnail',
      uploadedAt: '2026-06-01T10:00:00'
    },
    {
      versionId: 'ver-2',
      versionNumber: 2,
      fileName: 'sheet.xlsx',
      mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      fileSize: 2048,
      pageCount: null,
      status: 'READY',
      thumbnailUrl: '/api/documents/doc-1/versions/ver-2/thumbnail',
      uploadedAt: '2026-06-01T11:00:00'
    }
  ]
};

export const annotation: AnnotationResponse = {
  annotationId: 'ann-1',
  documentId: 'doc-1',
  versionId: 'ver-1',
  pageNumber: 1,
  annotationType: 'COMMENT',
  xPercent: 10,
  yPercent: 20,
  widthPercent: 30,
  heightPercent: 10,
  pixelX: 100,
  pixelY: 200,
  pixelWidth: 300,
  pixelHeight: 100,
  pageRenderWidth: 1000,
  pageRenderHeight: 1200,
  color: '#2563EB',
  strokeWidth: 2,
  selectedText: null,
  drawingData: null,
  commentText: 'Mentioned comment',
  createdByUserId: staffUser.userId,
  createdByName: staffUser.username,
  createdAt: '2026-06-01T12:00:00',
  updatedAt: '2026-06-01T12:00:00',
  comments: [
    {
      commentId: 'comment-1',
      annotationId: 'ann-1',
      commentText: 'Mentioned comment',
      createdByUserId: staffUser.userId,
      createdByName: staffUser.username,
      createdAt: '2026-06-01T12:00:00',
      updatedAt: '2026-06-01T12:00:00'
    }
  ]
};

export const notifications: NotificationResponse[] = [
  {
    notificationId: 'notification-1',
    notificationType: 'COMMENT_MENTION',
    title: 'Mention',
    message: 'staffuser mentioned you.',
    relatedDocumentId: 'doc-1',
    relatedVersionId: 'ver-1',
    relatedAnnotationId: 'ann-1',
    relatedCommentId: 'comment-1',
    read: false,
    createdAt: '2026-06-01T12:00:00'
  }
];

export const uploadResponse: DocumentUploadResponse = {
  documentId: 'doc-2',
  versionId: 'ver-3',
  versionNumber: 1,
  fileName: 'sample.pdf',
  mimeType: 'application/pdf',
  fileSize: 100,
  status: 'READY'
};

export function authServiceFor(currentUser: CurrentUserResponse | null) {
  const currentUserSignal = signal<CurrentUserResponse | null>(currentUser);
  return {
    currentUser: currentUserSignal,
    token: 'test-token',
    loadCurrentUser: jasmine.createSpy('loadCurrentUser').and.returnValue(of(currentUser)),
    listUsers: jasmine.createSpy('listUsers').and.returnValue(of([staffUser, supervisorUser])),
    listMentionableUsers: jasmine.createSpy('listMentionableUsers').and.returnValue(of([normalUser, staffUser, supervisorUser])),
    logout: jasmine.createSpy('logout')
  };
}
