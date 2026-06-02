import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  LucideHighlighter,
  LucideMessageSquarePlus,
  LucideMousePointer2,
  LucidePalette,
  LucidePencil,
  LucideSquare,
  LucideStrikethrough,
  LucideTrash2,
  LucideUnderline
} from '@lucide/angular';

import { AnnotationTool } from '../../../core/models/annotation.model';

interface ToolButton {
  tool: AnnotationTool;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-annotation-toolbar',
  standalone: true,
  imports: [
    LucideHighlighter,
    LucideMessageSquarePlus,
    LucideMousePointer2,
    LucidePalette,
    LucidePencil,
    LucideSquare,
    LucideStrikethrough,
    LucideTrash2,
    LucideUnderline
  ],
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
  protected isColorPickerOpen = false;

  protected readonly tools: ToolButton[] = [
    { tool: 'SELECT', label: 'Select', icon: 'Pointer' },
    { tool: 'HIGHLIGHT', label: 'Highlight', icon: 'Highlighter' },
    { tool: 'COMMENT', label: 'Comment', icon: 'Comment' },
    { tool: 'RECTANGLE', label: 'Rectangle', icon: 'Rectangle' },
    { tool: 'FREEHAND_DRAW', label: 'Freehand Draw', icon: 'Draw' },
    { tool: 'UNDERLINE', label: 'Underline', icon: 'Underline' },
    { tool: 'STRIKE_THROUGH', label: 'Strike-through', icon: 'Strike' }
  ];

  protected readonly colors = [
    { label: 'Yellow', value: '#FDE047' },
    { label: 'Amber', value: '#F59E0B' },
    { label: 'Red', value: '#DC2626' },
    { label: 'Blue', value: '#2563EB' },
    { label: 'Violet', value: '#7C3AED' },
    { label: 'Green', value: '#16A34A' },
    { label: 'Teal', value: '#0F766E' },
    { label: 'Black', value: '#111827' }
  ];

  protected selectColor(color: string): void {
    this.colorSelected.emit(color);
  }
}
