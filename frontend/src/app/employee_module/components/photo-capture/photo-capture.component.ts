import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnDestroy, Output, ViewChild, inject, signal } from '@angular/core';
import { ToastService } from '../../../shared/services/toast.service';

/** Photos are resized/compressed to at most this dimension (longest side) before being turned
    into base64 - keeps the payload sent to (and stored by) the backend small regardless of how
    large the original camera/gallery photo was. */
const MAX_DIMENSION_PX = 480;
const JPEG_QUALITY = 0.82;

/**
 * Employee photo picker with two explicit options - Upload (pick an existing file) or Capture
 * (take a new one right now via the device/webcam camera) - rather than relying on a plain
 * <input type="file"> alone, whose OS-native chooser doesn't make "take a new photo right now"
 * an equally obvious, one-tap option on every platform.
 *
 * Used via [(photoData)] two-way binding - the value is always either null or a base64 data-URI
 * string (e.g. "data:image/jpeg;base64,...").
 */
@Component({
  selector: 'app-photo-capture',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './photo-capture.component.html',
  styleUrl: './photo-capture.component.css'
})
export class PhotoCaptureComponent implements OnDestroy {
  private readonly toast = inject(ToastService);

  @Input() photoData: string | null = null;
  @Output() photoDataChange = new EventEmitter<string | null>();

  @ViewChild('fileInput') fileInputRef?: ElementRef<HTMLInputElement>;
  @ViewChild('videoEl') videoRef?: ElementRef<HTMLVideoElement>;

  readonly showCamera = signal(false);
  /** Starts on the back camera - more useful default for an employee ID-style photo (someone
      else usually takes it), with an explicit switch button for the front/selfie camera. */
  private facingMode: 'user' | 'environment' = 'environment';
  private stream: MediaStream | null = null;

  get isFrontCamera(): boolean {
    return this.facingMode === 'user';
  }

  triggerUpload(): void {
    this.fileInputRef?.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // allows picking the exact same file again later if removed and re-added
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      this.toast.error('Please choose an image file.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => this.compressAndSet(reader.result as string);
    reader.readAsDataURL(file);
  }

  async openCamera(): Promise<void> {
    if (!navigator.mediaDevices?.getUserMedia) {
      this.toast.error('Camera access is not available on this device/browser.');
      return;
    }
    this.facingMode = 'environment';
    this.showCamera.set(true);
    await this.startStream();
  }

  /** Switches between front (selfie) and back camera - stops the current stream and requests
      the other one, rather than trying to reconfigure a live stream's facingMode (which most
      browsers/devices don't support changing on an already-open track). */
  async switchCamera(): Promise<void> {
    this.facingMode = this.facingMode === 'user' ? 'environment' : 'user';
    this.stream?.getTracks().forEach(track => track.stop());
    await this.startStream();
  }

  private async startStream(): Promise<void> {
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: this.facingMode } });
      // The <video> element only exists once showCamera() flips the @if in the template, so the
      // stream is attached on the next tick, after Angular has actually rendered it.
      setTimeout(() => {
        if (this.videoRef) this.videoRef.nativeElement.srcObject = this.stream;
      });
    } catch {
      this.toast.error(
        this.facingMode === 'environment'
          ? 'Unable to access the back camera - your device may only have a front camera, or check camera permissions.'
          : 'Unable to access the camera - check your browser/device camera permissions.'
      );
    }
  }

  capturePhoto(): void {
    const video = this.videoRef?.nativeElement;
    if (!video) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (this.isFrontCamera) {
      // The front-camera PREVIEW is mirrored via CSS (see template) so it feels like looking in
      // a mirror while framing a selfie - completely standard for any camera UI. But the actual
      // SAVED photo should read correctly to anyone else looking at it (no reversed text on a
      // badge, hair parted on the correct side, etc.), so the capture itself flips it right back
      // before drawing - this is exactly why the user saw their captured photo "backwards" even
      // though the live preview looked normal to them while taking it.
      ctx?.translate(canvas.width, 0);
      ctx?.scale(-1, 1);
    }
    ctx?.drawImage(video, 0, 0);
    this.compressAndSet(canvas.toDataURL('image/jpeg', JPEG_QUALITY));
    this.closeCamera();
  }

  closeCamera(): void {
    this.stream?.getTracks().forEach(track => track.stop());
    this.stream = null;
    this.showCamera.set(false);
  }

  removePhoto(): void {
    this.photoData = null;
    this.photoDataChange.emit(null);
  }

  ngOnDestroy(): void {
    this.closeCamera();
  }

  /** Draws the source image onto a canvas capped at MAX_DIMENSION_PX (longest side), then
      re-encodes as JPEG - this is what actually keeps a multi-megabyte phone-camera photo from
      ever reaching the backend as-is. */
  private compressAndSet(dataUrl: string): void {
    const img = new Image();
    img.onload = () => {
      let { width, height } = img;
      if (width > height && width > MAX_DIMENSION_PX) {
        height = Math.round((height / width) * MAX_DIMENSION_PX);
        width = MAX_DIMENSION_PX;
      } else if (height > MAX_DIMENSION_PX) {
        width = Math.round((width / height) * MAX_DIMENSION_PX);
        height = MAX_DIMENSION_PX;
      }
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      canvas.getContext('2d')?.drawImage(img, 0, 0, width, height);
      const compressed = canvas.toDataURL('image/jpeg', JPEG_QUALITY);
      this.photoData = compressed;
      this.photoDataChange.emit(compressed);
    };
    img.src = dataUrl;
  }
}
