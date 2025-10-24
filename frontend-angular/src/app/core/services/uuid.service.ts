import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UuidService {

  generate(): string {
    return crypto.randomUUID();
  }
}
