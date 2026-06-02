import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AnnotationToolbarComponent } from './viewer/annotation-toolbar/annotation-toolbar.component';
import { CommentPanelComponent } from './viewer/comment-panel/comment-panel.component';
import { ViewerComponent } from './viewer/viewer.component';
import { AnnotationService } from '../core/services/annotation.service';
import { AuthService } from '../core/services/auth.service';
import { DocumentService } from '../core/services/document.service';
import { annotation, authServiceFor, documentDetails, normalUser, staffUser, supervisorUser } from '../testing/regression-test-data';

describe('annotation-regression', () => {
  it('renders annotation toolbar tools and emits selected tool/delete events', async () => {
    await TestBed.configureTestingModule({ imports: [AnnotationToolbarComponent] }).compileComponents();
    const fixture = TestBed.createComponent(AnnotationToolbarComponent);
    fixture.componentRef.setInput('activeTool', 'SELECT');
    fixture.componentRef.setInput('canDelete', true);
    fixture.detectChanges();

    const tools = fixture.nativeElement.querySelectorAll('.tool-button');
    expect(fixture.nativeElement.textContent).toContain('Highlight');
    expect(fixture.nativeElement.textContent).toContain('Freehand Draw');
    spyOn(fixture.componentInstance.toolSelected, 'emit');
    spyOn(fixture.componentInstance.deleteSelected, 'emit');

    tools[1].click();
    tools[tools.length - 1].click();

    expect(fixture.componentInstance.toolSelected.emit).toHaveBeenCalledWith('HIGHLIGHT');
    expect(fixture.componentInstance.deleteSelected.emit).toHaveBeenCalled();
  });

  it('opens comment panel, replies, deletes comments, and autocompletes @mentions', async () => {
    await TestBed.configureTestingModule({ imports: [CommentPanelComponent] }).compileComponents();
    const fixture = TestBed.createComponent(CommentPanelComponent);
    fixture.componentRef.setInput('annotations', [annotation]);
    fixture.componentRef.setInput('canDelete', true);
    fixture.componentRef.setInput('canReply', true);
    fixture.componentRef.setInput('mentionableUsers', [normalUser, staffUser, supervisorUser]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Mentioned comment');
    spyOn(fixture.componentInstance.commentDeleted, 'emit');
    spyOn(fixture.componentInstance.replyAdded, 'emit');

    fixture.nativeElement.querySelector('.comment-delete').click();
    expect(fixture.componentInstance.commentDeleted.emit).toHaveBeenCalledWith({
      commentId: 'comment-1',
      annotationId: 'ann-1'
    });

    const textarea = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    textarea.value = '@sup';
    textarea.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('supervisor');

    fixture.nativeElement.querySelector('.reply-mentions button').click();
    fixture.detectChanges();
    expect(textarea.value).toContain('@supervisor');

    fixture.nativeElement.querySelector('.reply-submit').click();
    expect(fixture.componentInstance.replyAdded.emit).toHaveBeenCalledWith({
      annotationId: 'ann-1',
      commentText: '@supervisor',
      mentionedUserIds: ['supervisor-1']
    });
  });

  it('loads PDF viewer metadata and saves a draft annotation for staff', async () => {
    const annotationService = {
      listAnnotations: jasmine.createSpy('listAnnotations').and.returnValue(of([])),
      createAnnotation: jasmine.createSpy('createAnnotation').and.returnValue(of(annotation)),
      createComment: jasmine.createSpy('createComment').and.returnValue(of(annotation.comments[0])),
      updateAnnotation: jasmine.createSpy('updateAnnotation').and.returnValue(of(annotation))
    };
    const fixture = await createViewerFixture(staffUser, annotationService);
    const component = fixture.componentInstance as any;

    component.draftAnnotation = {
      pageNumber: 1,
      annotationType: 'HIGHLIGHT',
      xPercent: 1,
      yPercent: 2,
      widthPercent: 3,
      heightPercent: 4,
      pixelX: 10,
      pixelY: 20,
      pixelWidth: 30,
      pixelHeight: 40,
      pageRenderWidth: 1000,
      pageRenderHeight: 1200,
      color: '#FDE047',
      strokeWidth: 2,
      selectedText: null,
      drawingData: null
    };
    component.draftComment = 'Please review';
    component.saveDraft();

    expect(annotationService.createAnnotation).toHaveBeenCalled();
    expect(component.annotations.length).toBe(1);
  });

  it('updates draft @mention autocomplete in the PDF viewer dialog', async () => {
    const fixture = await createViewerFixture(staffUser);
    const component = fixture.componentInstance as any;

    component.onDraftCommentInput('@nor', 4);
    expect(component.mentionSuggestions.map((user: any) => user.username)).toContain('normaluser');
    component.insertMention(normalUser);
    expect(component.draftComment).toContain('@normaluser');
  });
});

async function createViewerFixture(currentUser: any, annotationServiceOverride?: any): Promise<ComponentFixture<ViewerComponent>> {
  const annotationService = annotationServiceOverride ?? {
    listAnnotations: jasmine.createSpy('listAnnotations').and.returnValue(of([annotation])),
    createAnnotation: jasmine.createSpy('createAnnotation').and.returnValue(of(annotation)),
    createComment: jasmine.createSpy('createComment').and.returnValue(of(annotation.comments[0])),
    updateAnnotation: jasmine.createSpy('updateAnnotation').and.returnValue(of(annotation))
  };
  await TestBed.configureTestingModule({
    imports: [ViewerComponent],
    providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: (key: string) => key === 'documentId' ? 'doc-1' : 'ver-1' } } } },
      { provide: DocumentService, useValue: { getDocumentDetails: () => of(documentDetails), getViewUrl: () => 'view-url', getDownloadUrl: () => 'download-url' } },
      { provide: AnnotationService, useValue: annotationService },
      { provide: AuthService, useValue: authServiceFor(currentUser) }
    ]
  }).compileComponents();
  const fixture = TestBed.createComponent(ViewerComponent);
  fixture.detectChanges();
  return fixture;
}
