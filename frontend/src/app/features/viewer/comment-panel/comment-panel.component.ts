import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { AnnotationResponse } from '../../../core/models/annotation.model';

@Component({
  selector: 'app-comment-panel',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './comment-panel.component.html',
  styleUrl: './comment-panel.component.scss'
})
export class CommentPanelComponent {
  @Input() annotations: AnnotationResponse[] = [];
  @Input() selectedAnnotationId: string | null = null;
  @Output() annotationSelected = new EventEmitter<AnnotationResponse>();
  @Output() commentDeleted = new EventEmitter<{ commentId: string; annotationId: string }>();

  protected pageGroups(): Array<{ pageNumber: number; annotations: AnnotationResponse[] }> {
    const groups = new Map<number, AnnotationResponse[]>();
    for (const annotation of this.annotations) {
      const items = groups.get(annotation.pageNumber) ?? [];
      items.push(annotation);
      groups.set(annotation.pageNumber, items);
    }

    return [...groups.entries()]
      .sort(([firstPage], [secondPage]) => firstPage - secondPage)
      .map(([pageNumber, annotations]) => ({ pageNumber, annotations }));
  }

  protected commentText(annotation: AnnotationResponse): string {
    return annotation.comments[0]?.commentText || 'No comment';
  }

  protected deleteComment(event: Event, annotation: AnnotationResponse): void {
    event.stopPropagation();
    const comment = annotation.comments[0];
    if (!comment) {
      return;
    }
    this.commentDeleted.emit({
      commentId: comment.commentId,
      annotationId: annotation.annotationId
    });
  }
}
