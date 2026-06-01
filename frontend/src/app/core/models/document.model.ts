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
  createdAt: string;
  updatedAt: string;
}

export interface DocumentDetailsResponse {
  documentId: string;
  title: string;
  latestVersionNumber: number;
  versions: DocumentVersionResponse[];
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
