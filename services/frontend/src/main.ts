import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { APP_INITIALIZER } from '@angular/core';
import { AppComponent } from './app/app.component';
import { ConfigService } from './app/services/config.service';

function initializeApp(configService: ConfigService) {
  return () => configService.loadConfig();
}

bootstrapApplication(AppComponent, {
  providers: [
    provideHttpClient(),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      deps: [ConfigService],
      multi: true,
    },
  ],
}).catch((error) => console.error(error));
