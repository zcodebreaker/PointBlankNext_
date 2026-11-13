"use client";

import { useEffect, useRef } from "react";

export default function LogoutCompletion() {
  const completed = useRef(false);

  useEffect(() => {
    if (completed.current) return;
    completed.current = true;

    window.alert("Logout concluído.");
    window.location.replace("/");
  }, []);

  return (
    <div className="loading" role="status" aria-live="polite" aria-label="Concluindo logout">
      <div className="loader">
        <span className="loader" aria-hidden="true" />
        <div className="logout-loading-label">loading..</div>
      </div>
    </div>
  );
}
