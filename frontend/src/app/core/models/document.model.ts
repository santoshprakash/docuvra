export interface DocumentUploadResponse {
  documentId: string;
  versionId: string;
  versionNumber: number;
  fileName: string;
  mimeType: string;
  fileSize: number;
  status: string;
}

export interface UploadNewVersionResponse {
  documentId: string;
  versionId: string;
  versionNumber: number;
  fileName: string;
  mimeType: string;
  fileSize: number;
  status: string;
}

export interface DocumentListResponse {
  documentId: string;
  title: string;
  latestVersionNumber: number;
  latestVersionId: string | null;
  thumbnailUrl: string | null;
  uploadedByUserId: string | null;
  uploadedByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentDetailsResponse {
  documentId: string;
  title: string;
  latestVersionNumber: number;
  uploadedByUserId: string | null;
  uploadedByName: string;
  assignments: DocumentAssignmentResponse[];
  versions: DocumentVersionResponse[];
}

export interface DocumentAssignmentResponse {
  assignmentId: string;
  userId: string;
  username: string;
  email: string;
  role: UserRole;
  assignedAt: string;
  assignedByName: string | null;
}

export interface DocumentVersionResponse {
  versionId: string;
  versionNumber: number;
  fileName: string;
  mimeType: string;
  fileSize: number;
  pageCount: number | null;
  status: string;
  thumbnailUrl: string;
  uploadedAt: string;
}

export type OcrReason =
  | 'IMAGE_FILE'
  | 'SCANNED_PDF'
  | 'PDF_WITH_TEXT_LAYER'
  | 'OFFICE_DOCUMENT'
  | 'EXCEL_DOCUMENT'
  | 'USER_FORCED'
  | 'OCR_NOT_REQUIRED';

export interface OcrStatusResponse {
  ocrAvailable: boolean;
  ocrRequired: boolean;
  ocrCompleted: boolean;
  ocrEligible: boolean;
  ocrForced: boolean;
  originalFileType: string;
  originalMimeType: string;
  reason: OcrReason;
}

export interface DocumentSearchMatchResponse {
  pageNumber: number;
  matchedText: string;
  boxes: string[];
}
import { UserRole } from './auth.model';
