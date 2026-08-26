# Workforce Auth — System Flow

A visual map of how the modules connect. For setup/build instructions, see `README.md`.

## End-to-end flow

```mermaid
flowchart TD
    A[SUPER_ADMIN] -->|creates| B[Client Company]
    B -->|creates| C[CLIENT_ADMIN]
    C --> D[Departments & Designations]
    C --> E[Sites]
    C --> F[Employees]
    F -->|assigned to| E
    F -->|assigned to| D

    F --> G[Salary Structure]
    G -->|BASIC / DA / HRA / other components| G

    F --> H[Attendance]
    H -->|marked daily: Present / Half Day / On Leave / Absent| H

    C --> I[Paid Leave Settings]
    I -->|monthly allocation, carry-forward, max cap| J[Employee Paid Leave Balance]
    F -->|extra leave grants: Medical/Special/Emergency/Other| J
    J -->|carries forward month to month| J

    C --> K[Payroll Settings]
    K -->|EPF %, ESI %, Professional Tax| L

    H --> L[Monthly Attendance & Payment Report]
    G --> L
    J --> L
    L -->|Basic, DA, Gross, EPF, ESI, PT, Advance/Uniform, Allowance| M[Net Payment]
    L -->|preview in browser| N[Excel / PDF Download]

    C -->|only Client Admin can view/export| L
```

## Where each thing is configured

```mermaid
flowchart LR
    subgraph Setup [One-time / occasional setup]
        S1[Departments & Designations]
        S2[Sites]
        S3[Salary Structures & Components]
        S4[Paid Leave Settings]
        S5[Payroll Settings - EPF/ESI/PT]
    end

    subgraph Daily [Day to day]
        D1[Add / manage Employees]
        D2[Mark Attendance]
        D3[Grant Extra Paid Leave]
    end

    subgraph Monthly [Once a month]
        M1[Open Monthly Attendance & Payment Report]
        M2[Adjust Paid Leave / Advance-Uniform / Allowance if needed]
        M3[Download Excel or PDF]
    end

    Setup --> Daily --> Monthly
```

## Who can do what (simplified)

```mermaid
flowchart TD
    SA[SUPER_ADMIN] -->|manages| CC[All Client Companies]
    CA[CLIENT_ADMIN] -->|manages, within own company| EMP[Employees, Attendance, Salary, Leave, Payroll]
    EMP2[Employee - own login] -->|views only| OWN[Own Salary & Own Paid Leave]
```
