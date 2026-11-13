"use client";

import { useState } from "react";

type PasswordInputProps = {
  id: string;
  name: string;
  autoComplete: "current-password" | "new-password";
  placeholder: string;
  describedBy?: string;
  minLength?: number;
  maxLength?: number;
};

export default function PasswordInput({ id, name, autoComplete, placeholder, describedBy, minLength, maxLength = 16 }: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="auth-input password-input">
      <input
        id={id}
        name={name}
        type={visible ? "text" : "password"}
        minLength={minLength}
        maxLength={maxLength}
        autoComplete={autoComplete}
        placeholder={placeholder}
        aria-describedby={describedBy}
        required
      />
      <button
        type="button"
        className={`password-toggle${visible ? " is-visible" : ""}`}
        aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
        aria-pressed={visible}
        onClick={() => setVisible((current) => !current)}
      />
    </div>
  );
}
