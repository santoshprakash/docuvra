import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { AnnotationResponse } from '../../../core/models/annotation.model';
import { UserResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-comment-panel',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './comment-panel.component.html',
  styleUrl: './comment-panel.component.scss'
})
export class CommentPanelComponent {
  private static readonly normalUserId = '10000000-0000-0000-0000-000000000001';

  @Input() annotations: AnnotationResponse[] = [];
  @Input() selectedAnnotationId: string | null = null;
  @Input() canDelete = true;
  @Input() canReply = true;
  @Input() mentionableUsers: UserResponse[] = [];
  @Output() annotationSelected = new EventEmitter<AnnotationResponse>();
  @Output() commentDeleted = new EventEmitter<{ commentId: string; annotationId: string }>();
  @Output() replyAdded = new EventEmitter<{ annotationId: string; commentText: string; mentionedUserIds: string[] }>();

  protected replyTextByAnnotationId: Record<string, string> = {};
  protected mentionIdsByAnnotationId: Record<string, Set<string>> = {};
  protected suggestionsByAnnotationId: Record<string, UserResponse[]> = {};
  protected mentionStartByAnnotationId: Record<string, number> = {};

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

  protected authorName(annotation: AnnotationResponse): string {
    return annotation.comments[0]?.createdByName || annotation.createdByName || 'Staff';
  }

  protected staffComments(annotation: AnnotationResponse): AnnotationResponse['comments'] {
    return annotation.comments.filter(comment => !this.isNormalUserComment(comment));
  }

  protected normalUserComments(annotation: AnnotationResponse): AnnotationResponse['comments'] {
    return annotation.comments.filter(comment => this.isNormalUserComment(comment));
  }

  protected annotationMeta(annotation: AnnotationResponse): string {
    return `Page ${annotation.pageNumber} - ${this.prettyType(annotation.annotationType)}`;
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

  protected onReplyInput(annotationId: string, value: string, caretIndex: number | null): void {
    this.replyTextByAnnotationId[annotationId] = value;
    this.updateSuggestions(annotationId, caretIndex ?? value.length);
  }

  protected insertMention(annotationId: string, user: UserResponse): void {
    const text = this.replyTextByAnnotationId[annotationId] ?? '';
    const startIndex = this.mentionStartByAnnotationId[annotationId] ?? text.length;
    const before = text.slice(0, startIndex);
    const after = text.slice(startIndex).replace(/^@[A-Za-z0-9._-]*/, '');
    this.replyTextByAnnotationId[annotationId] = `${before}@${user.username} ${after}`.replace(/\s{2,}/g, ' ');
    this.mentionIdsByAnnotationId[annotationId] ??= new Set<string>();
    this.mentionIdsByAnnotationId[annotationId].add(user.userId);
    this.suggestionsByAnnotationId[annotationId] = [];
  }

  protected submitReply(annotationId: string): void {
    const commentText = (this.replyTextByAnnotationId[annotationId] ?? '').trim();
    if (!commentText) {
      return;
    }
    this.replyAdded.emit({
      annotationId,
      commentText,
      mentionedUserIds: [...(this.mentionIdsByAnnotationId[annotationId] ?? new Set<string>())]
    });
    this.replyTextByAnnotationId[annotationId] = '';
    this.mentionIdsByAnnotationId[annotationId] = new Set<string>();
    this.suggestionsByAnnotationId[annotationId] = [];
  }

  private updateSuggestions(annotationId: string, caretIndex: number): void {
    const text = this.replyTextByAnnotationId[annotationId] ?? '';
    const textBeforeCaret = text.slice(0, caretIndex);
    const match = /(^|\s)@([A-Za-z0-9._-]*)$/.exec(textBeforeCaret);
    if (!match) {
      this.suggestionsByAnnotationId[annotationId] = [];
      return;
    }
    const query = match[2].toLowerCase();
    this.mentionStartByAnnotationId[annotationId] = caretIndex - query.length - 1;
    this.suggestionsByAnnotationId[annotationId] = this.mentionableUsers
      .filter(user => user.username.toLowerCase().includes(query) || user.email.toLowerCase().includes(query))
      .slice(0, 8);
  }

  private prettyType(value: string): string {
    return value
      .toLowerCase()
      .split('_')
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private isNormalUserComment(comment: AnnotationResponse['comments'][number]): boolean {
    return comment.createdByUserId === CommentPanelComponent.normalUserId
      || comment.createdByName.toLowerCase().includes('normal');
  }
}
