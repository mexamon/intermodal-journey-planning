import React from 'react';
import Editor from 'react-simple-code-editor';
import Prism from 'prismjs';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-javascript';
import * as styles from './CodeEditorField.module.scss';

type CodeLanguage = 'json' | 'javascript';

type CodeEditorFieldProps = {
  value: string;
  onChange: (value: string) => void;
  language: CodeLanguage;
  placeholder?: string;
  className?: string;
  readOnly?: boolean;
  minHeight?: number;
};

const highlight = (code: string, language: CodeLanguage) => {
  const grammar = Prism.languages[language] ?? Prism.languages.javascript;
  return Prism.highlight(code, grammar, language);
};

export const CodeEditorField: React.FC<CodeEditorFieldProps> = ({
  value,
  onChange,
  language,
  placeholder,
  className,
  readOnly = false,
  minHeight = 140,
}) => (
  <Editor
    value={value}
    onValueChange={(next) => {
      if (!readOnly) {
        onChange(next);
      }
    }}
    highlight={(code) => highlight(code, language)}
    padding={12}
    className={`${styles.editorRoot} ${className ?? ''}`}
    textareaClassName={styles.editorTextarea}
    preClassName={styles.editorPre}
    style={{ minHeight }}
    readOnly={readOnly}
    placeholder={placeholder}
  />
);
