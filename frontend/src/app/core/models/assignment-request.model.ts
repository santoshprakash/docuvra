export interface AssignmentRequestResponse {
  requestId: string;
  documentId: string;
  documentTitle: string;
  requestedByUserId: string;
  requestedByUsername: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  requestedAt: string;
  reviewComment: string | null;
}
