'use client';

import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
  title?: string;
}

export const Card = ({ children, className = '', title }: CardProps) => {
  return (
    <div className={`bg-[var(--card-bg)] border border-[var(--border-color)] rounded-2xl p-6 shadow-sm ${className}`}>
      {title && (
        <h3 className="text-lg font-bold text-[var(--text-main)] mb-4 pb-2 border-b border-[var(--border-color)]">
          {title}
        </h3>
      )}
      {children}
    </div>
  );
};

export const CardSection = ({ children, className = '', title }: CardProps) => {
  return (
    <div className={`mb-8 last:mb-0 ${className}`}>
      {title && (
        <h4 className="text-sm font-bold text-[var(--text-muted)] mb-4 uppercase tracking-wider">
          {title}
        </h4>
      )}
      {children}
    </div>
  );
};
