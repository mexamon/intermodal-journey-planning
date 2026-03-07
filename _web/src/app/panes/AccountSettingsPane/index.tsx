import React, { useState } from "react";
import * as styles from "./AccountSettingsPane.module.scss";
import * as sharedStyles from "../Panes.module.scss";
import {
  FiUser,
  FiShield,
  FiBell,
  FiUpload,
  FiSmartphone,
  FiChrome,
  FiLogOut,
} from "react-icons/fi";
import { timezones, activeSessions } from "../../data/mockData";
import QRCode from "react-qr-code";
import { SelectField } from "../../components/forms/SelectField";

type SettingsTab = "profile" | "security" | "notifications";

const mockUser = {
  fullName: "Engin Mahmut",
  email: "enginmahmut86@gmail.com",
  avatarUrl: "https://github.com/mexamon.png",
};

export const AccountSettingsPane = () => {
  const [activeTab, setActiveTab] = useState<SettingsTab>("profile");
  const [user, setUser] = useState(mockUser);
  const [show2FASetup, setShow2FASetup] = useState(false);
  const [language, setLanguage] = useState("English");
  const [timezone, setTimezone] = useState(timezones[0] ?? "");

  const renderContent = () => {
    switch (activeTab) {
      case "profile":
        return (
          <div>
            <header className={sharedStyles.paneHeader}>
              <h2>My Profile</h2>
              <p>Update your photo and personal details.</p>
            </header>
            <div className={styles.sectionContent}>
              <div className={sharedStyles.formGroup}>
                <label>Profile Photo</label>
                <div className={styles.avatarUploader}>
                  <img src={user.avatarUrl} alt="User Avatar" />
                  <button>
                    <FiUpload /> Upload New
                  </button>
                </div>
              </div>
              <div className={sharedStyles.formGroup}>
                <label htmlFor="fullName">Full Name</label>
                <input
                  id="fullName"
                  type="text"
                  value={user.fullName}
                  onChange={(e) => setUser({ ...user, fullName: e.target.value })}
                  className={sharedStyles.formInput}
                />
              </div>
              <div className={sharedStyles.formGroup}>
                <label htmlFor="email">Email Address</label>
                <input
                  id="email"
                  type="email"
                  value={user.email}
                  className={sharedStyles.formInput}
                  disabled
                />
              </div>

              <hr className={styles.divider} />

              <div className={sharedStyles.formGroup}>
                <label htmlFor="language">Language</label>
                <SelectField
                  value={language}
                  options={[
                    "English",
                    "Türkçe",
                    "Deutsch",
                    "Français",
                    "Español",
                    "Português",
                    "Русский",
                    "العربية",
                    "中文",
                  ].map((option) => ({ value: option, label: option }))}
                  onChange={setLanguage}
                />
              </div>

              <div className={sharedStyles.formGroup}>
                <label htmlFor="timezone">Time Zone</label>
                <SelectField
                  value={timezone}
                  options={timezones.map((tz) => ({ value: tz, label: tz }))}
                  onChange={setTimezone}
                />
              </div>
              <button
                className={`${sharedStyles.actionButton} ${sharedStyles.primary}`}
              >
                Save Changes
              </button>
            </div>
          </div>
        );
      case "security":
        return (
          <div>
            <header className={sharedStyles.paneHeader}>
              <h2>Password & Security</h2>
              <p>
                Manage your password, two-factor authentication, and active
                sessions.
              </p>
            </header>
            <div className={`${styles.section} ${styles.subSection}`}>
              <h3 className={styles.sectionTitle}>Change Password</h3>
              <div className={sharedStyles.formGroup}>
                <label htmlFor="currentPassword">Current Password</label>
                <input
                  id="currentPassword"
                  type="password"
                  className={sharedStyles.formInput}
                />
              </div>
              <div className={sharedStyles.formGroup}>
                <label htmlFor="newPassword">New Password</label>
                <input
                  id="newPassword"
                  type="password"
                  className={sharedStyles.formInput}
                />
              </div>
              <button
                className={`${sharedStyles.actionButton} ${sharedStyles.primary}`}
              >
                Update Password
              </button>
            </div>
            <div className={`${styles.section} ${styles.subSection}`}>
              <h3 className={styles.sectionTitle}>Two-Factor Authentication</h3>
              {!show2FASetup ? (
                <>
                  <p className={styles.sectionDescription}>
                    Add an additional layer of security to your account by
                    enabling 2FA.
                  </p>
                  <button
                    onClick={() => setShow2FASetup(true)}
                    className={sharedStyles.actionButton}
                  >
                    Enable 2FA
                  </button>
                </>
              ) : (
                <div className={styles.twoFactorSetup}>
                  <p className={styles.sectionDescription}>
                    Scan the QR code with your authenticator app.
                  </p>
                  <div className={styles.qrCodeWrapper}>
                    <QRCode
                      value="otpauth://totp/Boilerrum:engin.mahmut?secret=KVKFKVKBIW235&issuer=Boilerrum"
                      size={160}
                    />
                  </div>
                  <div className={sharedStyles.formGroup}>
                    <label htmlFor="2fa-code">Verification Code</label>
                    <input
                      id="2fa-code"
                      placeholder="Enter 6-digit code"
                      className={sharedStyles.formInput}
                    />
                  </div>
                  <div className={styles.twoFactorActions}>
                    <button
                      onClick={() => setShow2FASetup(false)}
                      className={sharedStyles.actionButton}
                    >
                      Cancel
                    </button>
                    <button
                      className={`${sharedStyles.actionButton} ${sharedStyles.primary}`}
                    >
                      Verify & Activate
                    </button>
                  </div>
                </div>
              )}
            </div>
            <div className={`${styles.section} ${styles.subSection}`}>
              <h3 className={styles.sectionTitle}>Active Sessions</h3>
              <p className={styles.sectionDescription}>
                This is a list of devices that have logged into your account.
              </p>
              <ul className={styles.sessionList}>
                {activeSessions.map((session) => (
                  <li key={session.id}>
                    <div className={styles.sessionIcon}>
                      {session.browser.includes("iPhone") ? (
                        <FiSmartphone />
                      ) : (
                        <FiChrome />
                      )}
                    </div>
                    <div className={styles.sessionInfo}>
                      <span>
                        {session.browser} - IP: {session.ip}
                      </span>
                      <small>{session.lastSeen}</small>
                    </div>
                    {session.isCurrent ? (
                      <span className={styles.currentTag}>Current</span>
                    ) : (
                      <button className={styles.logoutButton}>
                        <FiLogOut /> Log out
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        );
      case "notifications":
        return (
          <div>
            <header className={sharedStyles.paneHeader}>
              <h2>Notifications</h2>
              <p>Manage how you receive notifications.</p>
            </header>
            <div className={sharedStyles.placeholder}>
              <p>Notification settings will be available here soon.</p>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  const navItems = [
    { id: "profile", label: "My Profile", icon: <FiUser /> },
    { id: "security", label: "Password & Security", icon: <FiShield /> },
    { id: "notifications", label: "Notifications", icon: <FiBell /> },
  ];

  return (
    <div className={styles.settingsWrapper}>
      <nav className={styles.settingsNav}>
        {navItems.map((item) => (
          <button
            key={item.id}
            className={`${styles.navItem} ${
              activeTab === item.id ? styles.active : ""
            }`}
            onClick={() => setActiveTab(item.id as SettingsTab)}
          >
            {item.icon}
            <span>{item.label}</span>
          </button>
        ))}
      </nav>
      <div className={styles.settingsContent}>{renderContent()}</div>
    </div>
  );
};
