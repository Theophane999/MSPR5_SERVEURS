import { Injectable } from '@angular/core';

export type ThemeName = 'default' | 'maroon';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private storageKey = 'app-theme';

  constructor() {
    const saved = localStorage.getItem(this.storageKey) as ThemeName | null;
    this.apply(saved ?? 'maroon');
  }

  setTheme(theme: ThemeName) {
    localStorage.setItem(this.storageKey, theme);
    this.apply(theme);
  }

  toggle() {
    const current = localStorage.getItem(this.storageKey) as ThemeName | null;
    const next: ThemeName = current === 'maroon' ? 'default' : 'maroon';
    this.setTheme(next);
  }

  current(): ThemeName {
    return (localStorage.getItem(this.storageKey) as ThemeName) ?? 'maroon';
  }

  private apply(theme: ThemeName) {
    const root = document.documentElement;
    if (theme === 'maroon') {
      root.classList.add('theme-maroon');
    } else {
      root.classList.remove('theme-maroon');
    }
  }
}
