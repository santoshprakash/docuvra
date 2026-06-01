import { Routes } from '@angular/router';
import { CompareComponent } from './features/compare/compare.component';
import { DocumentDetailsComponent } from './features/document-details/document-details.component';
import { DocumentListComponent } from './features/document-list/document-list.component';
import { ExcelViewerComponent } from './features/excel-viewer/excel-viewer.component';
import { ViewerComponent } from './features/viewer/viewer.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'documents'
  },
  {
    path: 'documents',
    component: DocumentListComponent
  },
  {
    path: 'documents/:documentId',
    component: DocumentDetailsComponent
  },
  {
    path: 'viewer/:documentId/:versionId',
    component: ViewerComponent
  },
  {
    path: 'excel-viewer/:documentId/:versionId',
    component: ExcelViewerComponent
  },
  {
    path: 'compare',
    component: CompareComponent
  }
];
