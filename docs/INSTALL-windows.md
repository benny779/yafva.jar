# Installation Guide for Windows

## Prerequisites
1. Download the required files from the [windows folder](../windows) in the project repository:
   - `yafvajar.exe`
   - `yafvajar.xml`
   - `application.yaml`
2. Alternatively, you can download the latest version of [WinSW](https://github.com/winsw/winsw/releases)
   and rename the binary to `yafvajar.exe`.
3. Ensure you have all three files: `yafvajar.exe`, `yafvajar.xml`, and `application.yaml`.

> **Note:** This guide uses WinSW version `2.12.0.0`.

## Installation Steps
1. Place the `yafvajar.exe`, `yafvajar.xml`, and `application.yaml` files
   in the same directory (e.g., `C:\yafva-validator`).
2. Open a command prompt with administrative privileges.
3. Navigate to the directory where the files are located.
4. Run the following command to install the service:
   ```
   yafvajar.exe install
   ```
5. Start the service with:
   ```
   yafvajar.exe start
   ```

## Notes
- To uninstall the service, use:
  ```
  yafvajar.exe uninstall
  ```

- For additional configuration options, refer to the [WinSW documentation](https://github.com/winsw/winsw).
