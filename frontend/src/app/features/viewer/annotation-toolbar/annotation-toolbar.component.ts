import { Component, EventEmitter, Input, Output } from '@angular/core';

import { AnnotationTool } from '../../../core/models/annotation.model';

interface ToolButton {
  tool: AnnotationTool;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-annotation-toolbar',
  standalone: true,
  templateUrl: './annotation-toolbar.component.html',
  styleUrl: './annotation-toolbar.component.scss'
})
export class AnnotationToolbarComponent {
  @Input({ required: true }) activeTool: AnnotationTool = 'SELECT';
  @Input() canDelete = false;
  @Input() selectedColor = '#2563EB';
  @Output() toolSelected = new EventEmitter<AnnotationTool>();
  @Output() deleteSelected = new EventEmitter<void>();
  @Output() colorSelected = new EventEmitter<string>();

  protected readonly tools: ToolButton[] = [
    { tool: 'SELECT', label: 'Select', icon: 'P' },
    { tool: 'HIGHLIGHT', label: 'Highlight', icon: 'H' },
    { tool: 'COMMENT', label: 'Comment', icon: 'C' },
    { tool: 'RECTANGLE', label: 'Rectangle', icon: 'R' },
    { tool: 'FREEHAND_DRAW', label: 'Freehand Draw', icon: 'D' },
    { tool: 'UNDERLINE', label: 'Underline', icon: 'U' },
    { tool: 'STRIKE_THROUGH', label: 'Strike-through', icon: 'S' }
  ];

  protected readonly colors = [
    { label: 'Yellow', value: '#FDE047' },
    { label: 'Red', value: '#DC2626' },
    { label: 'Blue', value: '#2563EB' },
    { label: 'Green', value: '#16A34A' },
    { label: 'Black', value: '#111827' }
  ];
}
