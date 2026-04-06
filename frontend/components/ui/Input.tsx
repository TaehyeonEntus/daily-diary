'use client';

import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className = '', label, error, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-semibold text-[var(--text-muted)] mb-1.5">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={`
            w-full px-4 py-2.5 
            bg-[var(--bg-color)] 
            border border-[var(--border-color)] 
            rounded-xl 
            text-[var(--text-main)] 
            placeholder:text-[var(--text-muted)] 
            outline-none 
            transition-all 
            focus:border-[var(--primary-color)] 
            focus:ring-2 
            focus:ring-[var(--primary-color)]/20 
            ${error ? 'border-red-500 focus:border-red-500 focus:ring-red-500/20' : ''}
            ${className}
          `}
          {...props}
        />
        {error && (
          <p className="mt-1 text-xs text-red-500 font-medium">{error}</p>
        )}
      </div>
    );
  }
);

Input.displayName = 'Input';
