#define MyAppName "GX Mod Downloader"
#define MyAppVersion "0.2.1-alpha"
#define MyAppPublisher "Alastor-Kaneki"
#define MyAppExeName "GX-Mod-Downloader.exe"

[Setup]
AppId={{50F976AD-3814-4C99-8615-6B937AE87E9E}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\GX Mod Downloader
DefaultGroupName=GX Mod Downloader
OutputDir=..\dist
OutputBaseFilename=GX-Mod-Downloader-0.2.1-windows-x64-setup
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\{#MyAppExeName}

[Files]
Source: "..\dist\windows-publish\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\GX Mod Downloader"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\GX Mod Downloader"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Launch GX Mod Downloader"; Flags: nowait postinstall skipifsilent
