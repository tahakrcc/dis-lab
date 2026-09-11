import { useRef, useState } from "react";

/**
 * Tarayıcı mikrofonuyla sesli not kaydeder (MediaRecorder). Kayıt bitince
 * onRecorded(file) çağrılır; dosya bir attachment olarak yüklenebilir.
 * Ekstra bağımlılık yok — yerleşik Web API.
 */
export function VoiceRecorder({
  onRecorded,
  disabled,
  etiket = "🎤 Sesli not",
}: {
  onRecorded: (file: File) => void;
  disabled?: boolean;
  etiket?: string;
}) {
  const [recording, setRecording] = useState(false);
  const [sure, setSure] = useState(0);
  const [hata, setHata] = useState<string | null>(null);
  const mrRef = useRef<MediaRecorder | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  async function basla() {
    setHata(null);
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined") {
      setHata("Tarayıcı ses kaydını desteklemiyor.");
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mime = MediaRecorder.isTypeSupported("audio/webm") ? "audio/webm" : "";
      const mr = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined);
      chunksRef.current = [];
      mr.ondataavailable = (e) => {
        if (e.data && e.data.size > 0) chunksRef.current.push(e.data);
      };
      mr.onstop = () => {
        stream.getTracks().forEach((t) => t.stop());
        const type = mr.mimeType || "audio/webm";
        const ext = type.includes("ogg") ? "ogg" : "webm";
        const blob = new Blob(chunksRef.current, { type });
        const file = new File([blob], `sesli-not-${Date.now()}.${ext}`, { type });
        onRecorded(file);
      };
      mr.start();
      mrRef.current = mr;
      setRecording(true);
      setSure(0);
      timerRef.current = setInterval(() => setSure((s) => s + 1), 1000);
    } catch {
      setHata("Mikrofona erişilemedi (izin gerekli).");
    }
  }

  function durdur() {
    try {
      mrRef.current?.stop();
    } catch {
      /* yut */
    }
    setRecording(false);
    if (timerRef.current) clearInterval(timerRef.current);
  }

  const mmss = `${String(Math.floor(sure / 60)).padStart(2, "0")}:${String(sure % 60).padStart(2, "0")}`;

  return (
    <span className="voice-rec">
      {!recording ? (
        <button type="button" className="btn btn-ghost" onClick={basla} disabled={disabled}>
          {etiket}
        </button>
      ) : (
        <button type="button" className="btn btn-danger" onClick={durdur}>
          <span className="rec-dot" /> Durdur · {mmss}
        </button>
      )}
      {hata && <span className="voice-err">{hata}</span>}
    </span>
  );
}
