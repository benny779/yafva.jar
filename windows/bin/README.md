# NSSM (Non-Sucking Service Manager)

This directory should contain the NSSM executable for Windows service management.

## Download Instructions

1. Download NSSM from the official website: **https://nssm.cc/download**
2. Extract the downloaded archive
3. Copy `nssm.exe` from the appropriate architecture folder to this directory:
   - For 64-bit Windows: use `win64\nssm.exe`
   - For 32-bit Windows: use `win32\nssm.exe`

## File Location

After downloading, your directory structure should look like:
```
windows/
├── bin/
│   ├── nssm.exe          <- Place the downloaded file here
│   └── README.md         <- This file
├── install-service.bat
├── uninstall-service.bat
├── application.yaml
└── yafva.jar
```

## Version Compatibility

NSSM 2.24 or later is recommended. The service installation scripts are compatible with all recent versions of NSSM.

## Why NSSM is not included

NSSM is a third-party executable and is not included in this repository to:
- Keep the repository size small
- Comply with licensing requirements
- Allow users to download the latest version directly from the official source
- Enable users to verify the binary's authenticity
