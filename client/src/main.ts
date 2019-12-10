import './polyfills.ts';

import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { enableProdMode } from '@angular/core';
import { AppModule } from './app/';

declare var DDP_ENV: any;

if (DDP_ENV.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule);
