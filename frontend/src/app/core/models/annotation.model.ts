export type AnnotationType =
  | 'HIGHLIGHT'
  | 'COMMENT'
  | 'RECTANGLE'
  | 'FREEHAND_DRAW'
  | 'UNDERLINE'
  | 'STRIKE_THROUGH';

export type AnnotationTool = AnnotationType | 'SELECT';

export interface AnnotationCommentResponse {
  commentId: string;
  annotationId: string;
  commentText: string;
  createdByUserId: string | null;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface AnnotationRequest {
  pageNumber: number;
  annotationType: AnnotationType;
  xPercent: number;
  yPercent: number;
  widthPercent: number;
  heightPercent: number;
  pixelX: number;
  pixelY: number;
  pixelWidth: number;
  pixelHeight: number;
  pageRenderWidth: number;
  pageRenderHeight: number;
  color: string;
  strokeWidth: number;
  selectedText?: string | null;
  drawingData?: string | null;
  createdByUserId?: string | null;
  createdByName?: string;
  commentText?: string | null;
  mentionedUserIds?: string[];
}

export interface AnnotationResponse extends AnnotationRequest {
  annotationId: string;
  documentId: string;
  versionId: string;
  createdAt: string;
  updatedAt: string;
  createdByUserId: string | null;
  createdByName: string;
  comments: AnnotationCommentResponse[];
}
