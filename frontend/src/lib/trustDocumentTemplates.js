import { COMPANY_META } from '../utils/companyMeta';

/**
 * Universal print handler that creates an iframe or pop-up print window
 */
export function printHtml(htmlContent, title = 'Document') {
  const printWindow = window.open('', '_blank', 'width=900,height=1000');
  if (!printWindow) {
    alert('Please allow popups to print documents.');
    return;
  }
  printWindow.document.open();
  printWindow.document.write(htmlContent);
  printWindow.document.close();
  printWindow.focus();
  setTimeout(() => {
    printWindow.print();
  }, 350);
}

/**
 * Standard Trust Agro Header HTML snippet
 */
export function getCompanyHeaderHtml(docTitleAmharic = '', docTitleEnglish = '') {
  return `
    <div style="border-bottom: 3px double #1b5e20; padding-bottom: 12px; margin-bottom: 18px; display: flex; justify-content: space-between; align-items: center;">
      <div>
        <div style="font-size: 20px; font-weight: 800; color: #1b5e20; letter-spacing: 0.5px;">TRUST Agro-consulting & Farming PLC</div>
        <div style="font-size: 14px; font-weight: 600; color: #e65100;">ትረስት አግሮ ኮንሰልቲንግ እና እርሻ ኃ/የተ/የግ/ማ</div>
        <div style="font-size: 11px; color: #4b5563; margin-top: 4px;">
          TIN: <strong>${COMPANY_META.contact.tin}</strong> | Office: ${COMPANY_META.contact.office} | Mob: ${COMPANY_META.contact.phone.join(' / ')}
        </div>
        <div style="font-size: 11px; color: #4b5563;">
          Email: ${COMPANY_META.contact.email} | Telegram: ${COMPANY_META.contact.telegram} | ${COMPANY_META.contact.address}
        </div>
      </div>
      <div style="text-align: right;">
        ${docTitleAmharic ? `<div style="font-size: 18px; font-weight: bold; color: #1b5e20;">${docTitleAmharic}</div>` : ''}
        ${docTitleEnglish ? `<div style="font-size: 13px; font-weight: 700; color: #e65100; text-transform: uppercase;">${docTitleEnglish}</div>` : ''}
        <div style="font-size: 11px; color: #6b7280; margin-top: 4px;">Date: ${new Date().toLocaleDateString('en-GB')}</div>
      </div>
    </div>
  `;
}

export function getBaseStyles(landscape = false) {
  return `
    <style>
      @page {
        size: ${landscape ? 'A4 landscape' : 'A4'};
        margin: ${landscape ? '10mm' : '14mm 16mm'};
      }
      * { box-sizing: border-box; }
      body {
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Nyala", sans-serif;
        color: #1f2937;
        background: #ffffff;
        margin: 0;
        padding: 20px;
        font-size: 12px;
        line-height: 1.45;
      }
      table {
        width: 100%;
        border-collapse: collapse;
        margin: 12px 0;
      }
      th, td {
        border: 1px solid #d1d5db;
        padding: 6px 8px;
        text-align: left;
        font-size: 11px;
      }
      th {
        background-color: #f3f4f6;
        color: #111827;
        font-weight: 600;
      }
      .table-striped tr:nth-child(even) td { background-color: #fafafa; }
      .text-center { text-align: center; }
      .text-right { text-align: right; }
      .font-bold { font-weight: bold; }
      .text-primary { color: #1b5e20; }
      .text-accent { color: #e65100; }
      .badge {
        display: inline-block;
        padding: 2px 6px;
        font-size: 10px;
        font-weight: 600;
        border-radius: 4px;
        background: #e5e7eb;
      }
      .badge-success { background: #dcfce7; color: #166534; }
      .badge-warning { background: #fef3c7; color: #92400e; }
      .signature-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 24px;
        margin-top: 36px;
        page-break-inside: avoid;
      }
      .signature-box {
        border-top: 1px dashed #4b5563;
        padding-top: 6px;
        text-align: center;
        font-size: 11px;
      }
      @media print {
        body { padding: 0; }
        .no-print { display: none !important; }
      }
    </style>
  `;
}

/**
 * 1. Employee Directory Template
 */
export function renderEmployeeDirectoryHtml(employees = []) {
  const rows = employees.map((emp, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td class="font-bold">${emp.fullName || emp.name || '-'}</td>
      <td>${emp.positionName || emp.position || '-'}</td>
      <td>${emp.phone || '-'}</td>
      <td class="text-center">
        <span class="badge ${emp.active !== false && emp.status !== 'INACTIVE' ? 'badge-success' : 'badge-warning'}">
          ${emp.active !== false && emp.status !== 'INACTIVE' ? 'Active' : 'Inactive'}
        </span>
      </td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Trust Agro - Employee Directory</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የሰራተኞች ማውጫ', 'Employee Directory')}
        <div style="margin-bottom: 12px; font-weight: 600; color: #374151;">
          Total Registered Employees: <strong>${employees.length}</strong>
        </div>
        <table class="table-striped">
          <thead>
            <tr>
              <th style="width: 50px;" class="text-center">No</th>
              <th>Full Name</th>
              <th>Position</th>
              <th>Phone Number</th>
              <th style="width: 90px;" class="text-center">Status</th>
            </tr>
          </thead>
          <tbody>
            ${rows || '<tr><td colspan="5" class="text-center">No employees found</td></tr>'}
          </tbody>
        </table>
        <div class="signature-grid">
          <div class="signature-box">
            <strong>Prepared by</strong><br/>
            <span>HR Department</span>
          </div>
          <div class="signature-box">
            <strong>Verified by</strong><br/>
            <span>Finance / Operations</span>
          </div>
          <div class="signature-box">
            <strong>Approved by</strong><br/>
            <span>Dr. Tekle Shiferaw (General Manager)</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 2. Training Attendance Template
 */
export function renderTrainingAttendanceHtml(session = {}, trainees = []) {
  const dates = session.sessionDates || ['Day 1', 'Day 2', 'Day 3'];
  const headerCols = dates.map(d => `<th colspan="2" class="text-center">${d}</th>`).join('');
  const subCols = dates.map(() => `<th class="text-center" style="font-size:9px;">AM</th><th class="text-center" style="font-size:9px;">PM</th>`).join('');

  const rows = trainees.map((t, idx) => {
    const checks = dates.map((_, i) => {
      const am = t.sessions?.[i]?.morning ? '✓' : '';
      const pm = t.sessions?.[i]?.afternoon ? '✓' : '';
      return `<td class="text-center">${am}</td><td class="text-center">${pm}</td>`;
    }).join('');

    return `
      <tr>
        <td class="text-center">${idx + 1}</td>
        <td class="font-bold">${t.fullName || t.traineeName || '-'}</td>
        <td>${t.phone || '-'}</td>
        ${checks}
        <td class="text-center">${t.certificateIssued ? '<span class="badge badge-success">Issued</span>' : 'Pending'}</td>
        <td style="border-bottom: 1px solid #ccc; width: 80px;"></td>
      </tr>
    `;
  }).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Training Attendance - Batch ${session.batchNo || session.batch || '72'}</title>
        ${getBaseStyles(true)}
      </head>
      <body>
        ${getCompanyHeaderHtml('የስልጠና ተሳታፊዎች መከታተያ', `Training Attendance - Batch ${session.batchNo || session.batch || '72'}`)}
        <div style="display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 11px;">
          <div><strong>Program:</strong> ${session.title || 'Poultry Farm Establishment & Management'}</div>
          <div><strong>Venue:</strong> ${session.venue || 'Addis Ababa Center'}</div>
          <div><strong>Batch:</strong> ${session.batchNo || '72'}</div>
        </div>
        <table>
          <thead>
            <tr>
              <th rowspan="2" style="width: 35px;" class="text-center">No</th>
              <th rowspan="2">Trainee Full Name</th>
              <th rowspan="2">Phone</th>
              ${headerCols}
              <th rowspan="2" class="text-center">Cert.</th>
              <th rowspan="2">Signature</th>
            </tr>
            <tr>
              ${subCols}
            </tr>
          </thead>
          <tbody>
            ${rows || '<tr><td colspan="10" class="text-center">No trainees registered</td></tr>'}
          </tbody>
        </table>
        <div class="signature-grid">
          <div class="signature-box">
            <strong>Training Coordinator</strong><br/>
            <span>Ms. Selamawit Mebratu</span>
          </div>
          <div class="signature-box">
            <strong>Lead Trainer</strong><br/>
            <span>Dr. Kemil Hayru / Dr. Mareag</span>
          </div>
          <div class="signature-box">
            <strong>General Manager</strong><br/>
            <span>Dr. Tekle Shiferaw</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 3. Certificate of Participation Template (A4 Landscape)
 */
export function renderCertificateHtml(data = {}) {
  const recipient = data.recipientName || data.traineeName || 'Recipient Name';
  const title = data.trainingTitle || 'Poultry Farm Establishment, Poultry Business Production and Management';
  const startDate = data.startDate || '01/10/2026';
  const endDate = data.endDate || '05/10/2026';
  const venue = data.venue || 'Addis Ababa, Ethiopia';
  const batchNo = data.batchNo || '73';

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Certificate - ${recipient}</title>
        <style>
          @page { size: A4 landscape; margin: 8mm; }
          * { box-sizing: border-box; }
          body {
            margin: 0;
            padding: 10px;
            font-family: 'Georgia', serif;
            color: #1f2937;
            background: #ffffff;
            display: flex;
            align-items: center;
            justify-content: center;
            height: 98vh;
          }
          .cert-outer {
            width: 100%;
            height: 100%;
            border: 8px solid #1b5e20;
            padding: 8px;
            background: #ffffff;
            position: relative;
          }
          .cert-inner {
            width: 100%;
            height: 100%;
            border: 3px solid #e65100;
            padding: 24px 36px;
            text-align: center;
            background: radial-gradient(circle at center, #ffffff 60%, #fdfbf7 100%);
            display: flex;
            flex-direction: column;
            justify-content: space-between;
          }
          .org-name {
            font-size: 26px;
            font-weight: 800;
            color: #1b5e20;
            letter-spacing: 1.5px;
            text-transform: uppercase;
          }
          .org-amharic {
            font-size: 16px;
            color: #e65100;
            font-weight: 600;
          }
          .cert-title {
            font-size: 32px;
            color: #1b5e20;
            font-weight: bold;
            letter-spacing: 2px;
            margin: 12px 0 6px 0;
            font-family: 'Times New Roman', serif;
            text-shadow: 1px 1px 1px rgba(0,0,0,0.1);
          }
          .cert-subtitle {
            font-size: 15px;
            color: #4b5563;
            font-style: italic;
          }
          .recipient {
            font-size: 30px;
            font-weight: bold;
            color: #0f3813;
            border-bottom: 2px solid #1b5e20;
            display: inline-block;
            padding: 4px 30px;
            margin: 10px auto;
            font-family: 'Palatino', serif;
          }
          .course-title {
            font-size: 18px;
            font-weight: 700;
            color: #c2410c;
            margin: 8px auto;
            max-width: 800px;
          }
          .meta-info {
            font-size: 13px;
            color: #4b5563;
          }
          .cert-signatures {
            display: flex;
            justify-content: space-between;
            margin-top: 20px;
            padding: 0 40px;
          }
          .sig-block {
            text-align: center;
            width: 240px;
          }
          .sig-line {
            border-top: 1.5px solid #1f2937;
            margin-top: 36px;
            padding-top: 6px;
            font-size: 13px;
            font-weight: bold;
          }
          .sig-role {
            font-size: 11px;
            color: #4b5563;
          }
          @media print {
            body { padding: 0; }
          }
        </style>
      </head>
      <body>
        <div class="cert-outer">
          <div class="cert-inner">
            <div>
              <div class="org-name">TRUST Agro-consulting & Farming PLC</div>
              <div class="org-amharic">ትረስት አግሮ ኮንሰልቲንግ እና እርሻ ኃ/የተ/የግ/ማ</div>
              <div style="font-size: 10px; color: #6b7280; margin-top: 2px;">TIN: ${COMPANY_META.contact.tin} | Addis Ababa, Ethiopia</div>
            </div>

            <div>
              <div class="cert-title">CERTIFICATE OF PARTICIPATION</div>
              <div class="cert-subtitle">This certificate is proudly awarded to</div>
              <div class="recipient">${recipient}</div>
              <div class="cert-subtitle">for actively participating and successfully completing the intensive training course on:</div>
              <div class="course-title">"${title}"</div>
              <div class="meta-info">
                Conducted from <strong>${startDate}</strong> to <strong>${endDate}</strong> at <strong>${venue}</strong> (Batch No: <strong>${batchNo}</strong>)
              </div>
            </div>

            <div class="cert-signatures">
              <div class="sig-block">
                <div class="sig-line">Ms. Selamawit Mebratu</div>
                <div class="sig-role">Training Coordinator</div>
              </div>
              <div style="display:flex; align-items:center; justify-content:center;">
                <div style="width: 72px; height: 72px; border: 3px double #1b5e20; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 9px; font-weight: bold; color: #1b5e20; text-align: center;">
                  OFFICIAL<br/>SEAL
                </div>
              </div>
              <div class="sig-block">
                <div class="sig-line">Dr. Tekle Shiferaw</div>
                <div class="sig-role">General Manager / Lead Consultant</div>
              </div>
            </div>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 4. Employee Attendance Sheet Template
 */
export function renderEmployeeAttendanceHtml(employee = {}, month = '', entries = []) {
  const currentMonth = month || new Date().toLocaleString('default', { month: 'long', year: 'numeric' });
  const rows = entries.map((entry, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td class="font-bold text-center">${entry.day || 'Mon'}</td>
      <td class="text-center">${entry.check_in_am || '08:30'}</td>
      <td class="text-center">${entry.check_out_am || '11:30'}</td>
      <td class="text-center">${entry.check_in_pm || '13:30'}</td>
      <td class="text-center">${entry.check_out_pm || '17:30'}</td>
      <td class="text-center">${entry.hoursWorked || '8'} hrs</td>
      <td style="border-bottom: 1px solid #ccc; width: 100px;"></td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Employee Attendance - ${employee.fullName || employee.name || 'Staff'}</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የሰራተኞች ወርሃዊ መገኘት መከታተያ', 'Employee Monthly Attendance')}
        <div style="display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 12px; border: 1px solid #e5e7eb; padding: 10px; border-radius: 6px; background: #fafafa;">
          <div><strong>Employee:</strong> ${employee.fullName || employee.name || '-'} (${employee.positionName || employee.position || '-'})</div>
          <div><strong>Month:</strong> ${currentMonth}</div>
          <div><strong>Standard Hours:</strong> AM: 08:30–11:30 | PM: 13:30–17:30</div>
        </div>
        <table class="table-striped">
          <thead>
            <tr>
              <th rowspan="2" class="text-center" style="width: 40px;">No</th>
              <th rowspan="2" class="text-center" style="width: 60px;">Day</th>
              <th colspan="2" class="text-center" style="background:#e8f5e9;">Morning Session (AM)</th>
              <th colspan="2" class="text-center" style="background:#fff3e0;">Afternoon Session (PM)</th>
              <th rowspan="2" class="text-center" style="width: 80px;">Total</th>
              <th rowspan="2" style="width: 100px;">Signature</th>
            </tr>
            <tr>
              <th class="text-center">In (08:30)</th>
              <th class="text-center">Out (11:30)</th>
              <th class="text-center">In (13:30)</th>
              <th class="text-center">Out (17:30)</th>
            </tr>
          </thead>
          <tbody>
            ${rows || `
              <tr>
                <td class="text-center">1</td>
                <td class="font-bold text-center">Mon</td>
                <td class="text-center">08:30</td><td class="text-center">11:30</td>
                <td class="text-center">13:30</td><td class="text-center">17:30</td>
                <td class="text-center">8 hrs</td><td></td>
              </tr>
              <tr>
                <td class="text-center">2</td>
                <td class="font-bold text-center">Tue</td>
                <td class="text-center">08:30</td><td class="text-center">11:30</td>
                <td class="text-center">13:30</td><td class="text-center">17:30</td>
                <td class="text-center">8 hrs</td><td></td>
              </tr>
            `}
          </tbody>
        </table>
        <div class="signature-grid">
          <div class="signature-box">
            <strong>Employee Signature</strong><br/>
            <span>${employee.fullName || 'Staff Member'}</span>
          </div>
          <div class="signature-box">
            <strong>Supervisor</strong><br/>
            <span>Department Lead</span>
          </div>
          <div class="signature-box">
            <strong>HR Manager</strong><br/>
            <span>Verified & Approved</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 5. Pullet Feed Schedule Template (Weeks 8 - 24)
 */
export const PULLET_FEED_SCHEDULE_DATA = [
  { week: 8, days: '50-56', std_intake_g: 45, recommended_g: 45, body_weight_g: '620-650', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 9, days: '57-63', std_intake_g: 49, recommended_g: 50, body_weight_g: '710-750', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 10, days: '64-70', std_intake_g: 53, recommended_g: 54, body_weight_g: '800-850', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 11, days: '71-77', std_intake_g: 57, recommended_g: 58, body_weight_g: '890-940', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 12, days: '78-84', std_intake_g: 61, recommended_g: 62, body_weight_g: '980-1030', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 13, days: '85-91', std_intake_g: 65, recommended_g: 66, body_weight_g: '1070-1120', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 14, days: '92-98', std_intake_g: 69, recommended_g: 70, body_weight_g: '1150-1200', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 15, days: '99-105', std_intake_g: 73, recommended_g: 74, body_weight_g: '1230-1280', feed_type: 'Pullet Developer', light_on: '06:00', light_off: '18:00' },
  { week: 16, days: '106-112', std_intake_g: 77, recommended_g: 78, body_weight_g: '1310-1360', feed_type: 'Pre-Lay Feed', light_on: '05:30', light_off: '18:30' },
  { week: 17, days: '113-119', std_intake_g: 81, recommended_g: 82, body_weight_g: '1390-1440', feed_type: 'Pre-Lay Feed', light_on: '05:00', light_off: '19:00' },
  { week: 18, days: '120-126', std_intake_g: 85, recommended_g: 86, body_weight_g: '1460-1510', feed_type: 'Pre-Lay Feed', light_on: '04:30', light_off: '19:30' },
  { week: 19, days: '127-133', std_intake_g: 90, recommended_g: 92, body_weight_g: '1530-1580', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
  { week: 20, days: '134-140', std_intake_g: 95, recommended_g: 98, body_weight_g: '1600-1650', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
  { week: 21, days: '141-147', std_intake_g: 100, recommended_g: 104, body_weight_g: '1660-1710', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
  { week: 22, days: '148-154', std_intake_g: 105, recommended_g: 108, body_weight_g: '1720-1760', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
  { week: 23, days: '155-161', std_intake_g: 110, recommended_g: 112, body_weight_g: '1770-1810', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
  { week: 24, days: '162-168', std_intake_g: 112, recommended_g: 115, body_weight_g: '1820-1860', feed_type: 'Layer Phase 1', light_on: '04:00', light_off: '20:00' },
];

export function renderPulletFeedScheduleHtml(customSchedule = null) {
  const schedule = customSchedule || PULLET_FEED_SCHEDULE_DATA;
  const rows = schedule.map(row => `
    <tr>
      <td class="text-center font-bold">${row.week}</td>
      <td class="text-center">${row.days}</td>
      <td class="text-right">${row.std_intake_g}</td>
      <td class="text-right font-bold text-primary">${row.recommended_g}</td>
      <td class="text-center">${row.body_weight_g}</td>
      <td>${row.feed_type}</td>
      <td class="text-center">${row.light_on || '06:00'}</td>
      <td class="text-center">${row.light_off || '18:00'}</td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Pullet Feeding Schedule (Weeks 8–24)</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የፖሌት (ቄብ) መኖ መመገቢያ ፕሮግራም', 'Pullet Feeding Schedule (Weeks 8–24)')}
        <div style="margin-bottom: 12px; font-size: 11px; color: #4b5563;">
          Standard management guideline for Bovans Brown pullets from Week 8 to Week 24.
        </div>
        <table class="table-striped">
          <thead>
            <tr>
              <th class="text-center">Week</th>
              <th class="text-center">Age (Days)</th>
              <th class="text-right">Std Intake (g/bird)</th>
              <th class="text-right">Recommended (g/bird)</th>
              <th class="text-center">Target Weight (g)</th>
              <th>Feed Type</th>
              <th class="text-center">Light On</th>
              <th class="text-center">Light Off</th>
            </tr>
          </thead>
          <tbody>
            ${rows}
          </tbody>
        </table>
        <div style="background: #e8f5e9; border: 1px solid #c8e6c9; padding: 10px; border-radius: 6px; font-size: 11px; margin-top: 16px;">
          <strong>Feeding Notes:</strong>
          <ul style="margin: 4px 0 0 16px; padding: 0;">
            <li>Transition between feed types over a 3-day blend (75/25, 50/50, 25/75).</li>
            <li>Weigh 100 sample birds weekly to confirm weight matches target trajectory.</li>
            <li>Maintain clean, cool drinking water at all times; monitor daily water-to-feed ratio.</li>
          </ul>
        </div>
      </body>
    </html>
  `;
}

/**
 * 6. Vaccination Program Template
 */
export function renderVaccinationProgramHtml(records = [], futureSchedule = null) {
  const future = futureSchedule || [
    { age: '18-19 wks', vaccine: 'IB (Infectious Bronchitis)' },
    { age: '20-21 wks', vaccine: 'IB (Infectious Bronchitis)' },
    { age: 'Every 1 month', vaccine: 'Lasota (Newcastle)' },
    { age: 'Every 2 month', vaccine: 'IB (Infectious Bronchitis)' },
  ];

  const recordRows = records.map((r, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td>${r.bird_age || r.age || '-'}</td>
      <td>${r.date_given || r.administeredDate || '-'}</td>
      <td class="font-bold">${r.vaccine_type || r.vaccineName || '-'}</td>
      <td>${r.method || '-'}</td>
      <td>${r.disease || '-'}</td>
      <td>${r.administered_by || r.administeredBy || '-'}</td>
    </tr>
  `).join('');

  const futureRows = future.map(f => `
    <tr>
      <td class="font-bold">${f.age}</td>
      <td class="text-primary font-bold">${f.vaccine}</td>
      <td>Routine Booster / Production maintenance</td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Trust Agro - Vaccination Program</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የክትባት ፕሮግራም እና መዝገብ', 'Veterinary Vaccination Program')}
        
        <h3 style="color: #1b5e20; margin-bottom: 6px;">1. Administered Vaccination Records</h3>
        <table class="table-striped">
          <thead>
            <tr>
              <th class="text-center" style="width: 35px;">No</th>
              <th>Bird Age</th>
              <th>Date Given</th>
              <th>Vaccine Type</th>
              <th>Method</th>
              <th>Disease Prevented</th>
              <th>Administered By</th>
            </tr>
          </thead>
          <tbody>
            ${recordRows || '<tr><td colspan="7" class="text-center">No vaccination logs recorded</td></tr>'}
          </tbody>
        </table>

        <h3 style="color: #e65100; margin-top: 20px; margin-bottom: 6px;">2. Production Future Booster Schedule</h3>
        <table class="table-striped">
          <thead>
            <tr>
              <th>Age / Frequency</th>
              <th>Vaccine Required</th>
              <th>Objective</th>
            </tr>
          </thead>
          <tbody>
            ${futureRows}
          </tbody>
        </table>

        <div class="signature-grid">
          <div class="signature-box">
            <strong>Veterinary Doctor</strong><br/>
            <span>Dr. Kemil Hayru / Dr. Tarekegn</span>
          </div>
          <div class="signature-box">
            <strong>Farm Manager</strong><br/>
            <span>Daniel Kibre</span>
          </div>
          <div class="signature-box">
            <strong>Technical Director</strong><br/>
            <span>Dr. Abunu Andarge</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 7. Proforma Invoice Template
 */
export function renderProformaInvoiceHtml(data = {}) {
  const items = data.items || [
    { description: '9-Week Bovans Brown Pullets (ቄብ ዶሮ)', unit: 'PCS', quantity: 1000, unit_price: 650, total_price: 650000 },
  ];
  const subTotal = data.sub_total || items.reduce((acc, it) => acc + (it.total_price || (it.quantity * it.unit_price)), 0);
  const vatRate = data.vat_rate ?? 0.15;
  const vat = data.vat ?? (subTotal * vatRate);
  const grandTotal = data.grand_total ?? (subTotal + vat);
  const paymentTerms = data.payment_terms || '80% advance via bank transfer';
  const validityDays = data.validity_days || 7;

  const itemRows = items.map((it, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td>${it.description}</td>
      <td class="text-center">${it.unit || 'PCS'}</td>
      <td class="text-right">${Number(it.quantity).toLocaleString()}</td>
      <td class="text-right">${Number(it.unit_price).toLocaleString()} ETB</td>
      <td class="text-right font-bold">${Number(it.total_price || (it.quantity * it.unit_price)).toLocaleString()} ETB</td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Proforma Invoice - ${data.proforma_no || 'PRO-001'}</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የፕሮፎርማ ደረሰኝ', 'PROFORMA INVOICE')}

        <div style="display: flex; justify-content: space-between; margin-bottom: 16px; border: 1px solid #e5e7eb; padding: 12px; border-radius: 6px;">
          <div>
            <div style="font-size: 11px; color: #6b7280;">CUSTOMER DETAILS:</div>
            <div style="font-size: 14px; font-weight: bold; color: #111827;">${data.customer_name || 'Valued Customer'}</div>
            <div style="font-size: 12px; color: #374151;">Phone: ${data.customer_phone || '-'}</div>
            ${data.customer_tin ? `<div style="font-size: 12px; color: #374151;">TIN: ${data.customer_tin}</div>` : ''}
          </div>
          <div style="text-align: right;">
            <div style="font-size: 11px; color: #6b7280;">PROFORMA NO:</div>
            <div style="font-size: 14px; font-weight: bold; color: #1b5e20;">${data.proforma_no || 'PI-' + Date.now().toString().slice(-6)}</div>
            <div style="font-size: 12px; color: #374151;">Date: ${data.date || new Date().toLocaleDateString('en-GB')}</div>
            <div style="font-size: 11px; color: #e65100;">Validity: ${validityDays} Days</div>
          </div>
        </div>

        <table class="table-striped">
          <thead>
            <tr>
              <th class="text-center" style="width: 35px;">No</th>
              <th>Description</th>
              <th class="text-center" style="width: 60px;">Unit</th>
              <th class="text-right" style="width: 70px;">Qty</th>
              <th class="text-right" style="width: 110px;">Unit Price</th>
              <th class="text-right" style="width: 130px;">Total Price</th>
            </tr>
          </thead>
          <tbody>
            ${itemRows}
          </tbody>
          <tfoot>
            <tr>
              <td colspan="5" class="text-right font-bold">Sub-Total:</td>
              <td class="text-right font-bold">${subTotal.toLocaleString()} ETB</td>
            </tr>
            <tr>
              <td colspan="5" class="text-right font-bold">VAT (15%):</td>
              <td class="text-right font-bold">${vat.toLocaleString()} ETB</td>
            </tr>
            <tr style="background: #e8f5e9;">
              <td colspan="5" class="text-right font-bold" style="font-size: 13px; color: #1b5e20;">Grand Total:</td>
              <td class="text-right font-bold" style="font-size: 13px; color: #1b5e20;">${grandTotal.toLocaleString()} ETB</td>
            </tr>
          </tfoot>
        </table>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 14px;">
          <div style="border: 1px solid #d1d5db; border-radius: 6px; padding: 10px; font-size: 11px;">
            <strong style="color: #1b5e20;">Product Specification:</strong>
            <ul style="margin: 4px 0 0 16px; padding: 0;">
              <li><strong>Breed:</strong> Bovans Brown</li>
              <li><strong>Sex:</strong> 100% Female</li>
              <li><strong>Health:</strong> Vaccinated at hatchery and farm under veterinary supervision</li>
            </ul>
            <div style="margin-top: 6px;"><strong>Payment Terms:</strong> ${paymentTerms}</div>
          </div>

          <div style="border: 1px solid #d1d5db; border-radius: 6px; padding: 10px; font-size: 11px; background: #fafafa;">
            <strong style="color: #e65100;">CBE Bank Accounts for Transfer:</strong>
            <div style="margin-top: 4px;">
              ${COMPANY_META.bank_accounts.map(b => `
                <div style="margin-bottom: 3px;">
                  • <strong>${b.name}:</strong> <code>${b.no}</code>
                </div>
              `).join('')}
            </div>
          </div>
        </div>

        <div class="signature-grid">
          <div class="signature-box">
            <strong>Prepared by</strong><br/>
            <span>Sales & Commercial Officer</span>
          </div>
          <div class="signature-box">
            <strong>Checked by</strong><br/>
            <span>Accountant / Finance</span>
          </div>
          <div class="signature-box">
            <strong>Approved by</strong><br/>
            <span>General Manager</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 8. Pullet Daily Record Template
 */
export function renderPulletDailyRecordHtml(flock = {}, dailyRecords = []) {
  const rows = dailyRecords.map((r, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td>${r.date || '-'}</td>
      <td class="text-center">${r.age_days ?? r.ageDays ?? '-'}</td>
      <td class="text-right">${r.feed_given_kg ?? '-'}</td>
      <td class="text-right font-bold">${r.feed_consumed_kg ?? r.feedConsumed ?? '-'}</td>
      <td class="text-right" style="color:#d32f2f;">${r.morning_mortality ?? '-'}</td>
      <td class="text-right font-bold" style="color:#b71c1c;">${r.mortality ?? 0}</td>
      <td class="text-right">${r.culls ?? r.culledChickens ?? 0}</td>
      <td>${r.medication || '-'}</td>
      <td>${r.vaccine || '-'}</td>
      <td class="text-right">${r.water_liters ?? r.waterConsumed ?? '-'}</td>
      <td>${r.notes || r.symptomsOrRemarks || ''}</td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Pullet Daily Recording Sheet</title>
        ${getBaseStyles(true)}
      </head>
      <body>
        ${getCompanyHeaderHtml('የቄብ ዶሮ ዕለታዊ መዝገብ', 'Pullet Daily Production & Health Record')}
        
        <div style="display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 11px;">
          <div><strong>Farm / Barn:</strong> ${flock.farmName || flock.farm?.farmName || '-'} / ${flock.barnName || '-'}</div>
          <div><strong>Batch Code:</strong> ${flock.batchCode || '-'}</div>
          <div><strong>Breed:</strong> ${flock.breed || 'Bovans Brown'}</div>
          <div><strong>Current Count:</strong> ${(flock.currentBirdCount || flock.initialBirdCount || 0).toLocaleString()}</div>
        </div>

        <table class="table-striped">
          <thead>
            <tr>
              <th rowspan="2" class="text-center" style="width: 30px;">No</th>
              <th rowspan="2">Date</th>
              <th rowspan="2" class="text-center">Age (d)</th>
              <th colspan="2" class="text-center" style="background:#e8f5e9;">Feed (kg)</th>
              <th colspan="3" class="text-center" style="background:#fee2e2;">Losses & Culls</th>
              <th rowspan="2">Medication</th>
              <th rowspan="2">Vaccine</th>
              <th rowspan="2" class="text-right">Water (L)</th>
              <th rowspan="2">Notes / Symptoms</th>
            </tr>
            <tr>
              <th class="text-right">Given</th>
              <th class="text-right">Consumed</th>
              <th class="text-right">Morning Mort.</th>
              <th class="text-right">Total Mort.</th>
              <th class="text-right">Culls</th>
            </tr>
          </thead>
          <tbody>
            ${rows || '<tr><td colspan="12" class="text-center">No daily logs found</td></tr>'}
          </tbody>
        </table>

        <div class="signature-grid">
          <div class="signature-box">
            <strong>Farm Worker / Attendant</strong><br/>
            <span>Signature & Date</span>
          </div>
          <div class="signature-box">
            <strong>Farm Manager</strong><br/>
            <span>Daniel Kibre / Dr. Kemil</span>
          </div>
          <div class="signature-box">
            <strong>Veterinarian</strong><br/>
            <span>Health Clearance</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 9. Quotation Cover Template (የዋጋ ማቅረቢያ)
 */
export function renderQuotationCoverHtml(quotation = {}) {
  const titleAmharic = quotation.title_amharic || 'የዋጋ ማቅረቢያ';
  const preparedBy = quotation.prepared_by || 'Dr. Mareag Amare (Sales & Technical Adviser)';
  const checkedBy = quotation.checked_by || 'Dr. Tekle Shiferaw (General Manager)';
  const items = quotation.items || [
    { description: '9-Week Vaccinated Bovans Brown Pullet (ቄብ ዶሮ)', unit: 'PCS', quantity: 1500, unit_price: 650, total_price: 975000 },
  ];
  const subTotal = items.reduce((acc, it) => acc + (it.total_price || (it.quantity * it.unit_price)), 0);

  const rows = items.map((it, idx) => `
    <tr>
      <td class="text-center">${idx + 1}</td>
      <td class="font-bold">${it.description}</td>
      <td class="text-center">${it.unit || 'PCS'}</td>
      <td class="text-right">${Number(it.quantity).toLocaleString()}</td>
      <td class="text-right">${Number(it.unit_price).toLocaleString()} ETB</td>
      <td class="text-right font-bold">${Number(it.total_price || (it.quantity * it.unit_price)).toLocaleString()} ETB</td>
    </tr>
  `).join('');

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Quotation - ${titleAmharic}</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml(titleAmharic, 'Official Quotation / Price Offer')}

        <div style="display: flex; justify-content: space-between; margin-bottom: 16px; border: 1px solid #d1d5db; padding: 12px; border-radius: 6px;">
          <div>
            <div style="font-size: 11px; color: #6b7280;">ለተከበሩ / To:</div>
            <div style="font-size: 14px; font-weight: bold; color: #111827;">${quotation.client_name || quotation.customerName || 'Customer'}</div>
            <div style="font-size: 12px; color: #374151;">Phone: ${quotation.phone || quotation.customerPhone || '-'}</div>
            <div style="font-size: 12px; color: #374151;">Address: ${quotation.address || 'Addis Ababa / Regional'}</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 11px; color: #6b7280;">Ref / ቁጥር:</div>
            <div style="font-size: 13px; font-weight: bold; color: #1b5e20;">${quotation.quotation_no || 'QUO-' + Date.now().toString().slice(-6)}</div>
            <div style="font-size: 12px; color: #374151;">Date: ${quotation.date || new Date().toLocaleDateString('en-GB')}</div>
          </div>
        </div>

        <div style="margin: 12px 0; font-size: 13px; font-weight: bold; color: #1b5e20; text-align: center;">
          ጉዳዩ፡- ${titleAmharic} ማቅረብን ይመለከታል
        </div>

        <div style="font-size: 12px; line-height: 1.6; margin-bottom: 12px;">
          ድርጅታችን ትረስት አግሮ ኮንሰልቲንግ እና እርሻ ኃ/የተ/የግ/ማህበር በጥራት ደረጃቸው ከፍተኛ የሆኑ የዶሮ ዝርያዎችን፣ የዶሮ መኖ እንዲሁም የህክምናና ክትባት አገልግሎቶችን በታማኝነት በማቅረብ ላይ ይገኛል። በዚህም መሠረት የጠየቁትን የዋጋ ዝርዝር ከዚህ በታች አቅርበናል።
        </div>

        <table class="table-striped">
          <thead>
            <tr>
              <th class="text-center" style="width: 35px;">ተ.ቁ</th>
              <th>የእቃው / አገልግሎቱ ዝርዝር (Description)</th>
              <th class="text-center" style="width: 60px;">መለኪያ</th>
              <th class="text-right" style="width: 70px;">ብዛት</th>
              <th class="text-right" style="width: 110px;">የአንዱ ዋጋ</th>
              <th class="text-right" style="width: 130px;">ጠቅላላ ዋጋ</th>
            </tr>
          </thead>
          <tbody>
            ${rows}
          </tbody>
          <tfoot>
            <tr style="background: #e8f5e9;">
              <td colspan="5" class="text-right font-bold" style="font-size: 13px; color: #1b5e20;">ድምር (Total):</td>
              <td class="text-right font-bold" style="font-size: 13px; color: #1b5e20;">${subTotal.toLocaleString()} ETB</td>
            </tr>
          </tfoot>
        </table>

        <div style="margin-top: 14px; border: 1px solid #d1d5db; padding: 10px; border-radius: 6px; font-size: 11px; background: #fafafa;">
          <strong>አጠቃላይ ማስታወሻ እና የክፍያ ሁኔታ (Terms & Conditions):</strong>
          <ul style="margin: 4px 0 0 16px; padding: 0;">
            <li>ዋጋው በፕሮፎርማው ላይ ለተመለከተው ጊዜ (7 ቀናት) ብቻ ፀንቶ ይቆያል።</li>
            <li>ክፍያ በባንክ ዝውውር በድርጅታችን ወይም በባለቤቶቹ የኢትዮጵያ ንግድ ባንክ (CBE) ሂሳብ ይፈጸማል።</li>
            <li>ዶሮዎቹ ሙሉ ክትባት የተከተቡ እና በከፍተኛ ጤንነት ላይ ያሉ ናቸው።</li>
          </ul>
        </div>

        <div class="signature-grid" style="grid-template-columns: 1fr 1fr; margin-top: 40px;">
          <div class="signature-box">
            <strong>ያዘጋጀው / Prepared by:</strong><br/>
            <span>${preparedBy}</span>
          </div>
          <div class="signature-box">
            <strong>ያረጋገጠው / Checked by:</strong><br/>
            <span>${checkedBy}</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 10. Pullet Sale Contract Template (የዶሮ ሸያጭ ውል)
 */
export function renderPulletSaleContractHtml(contract = {}) {
  const seller = contract.seller || {
    name: 'TRUST Agro-consulting & Farming PLC',
    address: 'Addis Ababa, Ethiopia',
    phone: '0919076607 / 0919074407',
  };
  const buyer = contract.buyer || {
    name: contract.customerName || 'Buyer Name',
    address: contract.customerAddress || 'Addis Ababa',
    phone: contract.customerPhone || '09xxxxxxxx',
  };
  const birds = contract.birds || {
    age: '9-Week',
    breed: 'Bovans Brown',
    quantity: contract.quantity || 1000,
    unit_price: contract.unitPrice || 650,
  };
  const advancePercent = contract.advance_percent ?? 0.8;
  const advanceAmount = (birds.quantity * birds.unit_price) * advancePercent;
  const remainingAmount = (birds.quantity * birds.unit_price) * (1 - advancePercent);

  const freePackage = contract.free_package || [
    'Free debeaking',
    'Free vaccination',
    'Free transport',
    'Free chemical spray',
    'Free consultancy',
    'Free road accident insurance',
  ];

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>የዶሮ ሸያጭ ውል - Pullet Sale Contract</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የዶሮ ሸያጭ ውል', 'Pullet Sale Agreement')}

        <div style="text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 12px; color: #1b5e20;">
          በሻጭ እና በገዥ መካከል የተደረገ የቄብ ዶሮዎች (Pullets) የሽያጭ ውል ስምምነት
        </div>

        <div style="font-size: 11.5px; line-height: 1.6; margin-bottom: 12px;">
          ይህ ውል ዛሬ ቀን <strong>${contract.contract_date || new Date().toLocaleDateString('en-GB')}</strong> በ 
          <strong>${seller.name}</strong> (ከዚህ በኋላ "ሻጭ" እየተባለ በሚጠራው) እና በ 
          <strong>${buyer.name}</strong> (አድራሻ፡ ${buyer.address}፣ ስልክ፡ ${buyer.phone}) (ከዚህ በኋላ "ገዥ" እየተባለ በሚጠራው) መካከል ተፈረመ።
        </div>

        <h4 style="color: #1b5e20; margin: 8px 0 4px 0;">አንቀጽ 1. የዶሮዎች አይነትና ብዛት (Specifications)</h4>
        <table class="table-striped">
          <thead>
            <tr>
              <th>የዶሮ ዝርያ (Breed)</th>
              <th>እድሜ (Age)</th>
              <th class="text-right">ብዛት (Qty)</th>
              <th class="text-right">የአንዱ ዋጋ (Unit Price)</th>
              <th class="text-right">ጠቅላላ ዋጋ (Total)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="font-bold">${birds.breed}</td>
              <td>${birds.age}</td>
              <td class="text-right">${birds.quantity.toLocaleString()}</td>
              <td class="text-right">${birds.unit_price.toLocaleString()} ETB</td>
              <td class="text-right font-bold">${(birds.quantity * birds.unit_price).toLocaleString()} ETB</td>
            </tr>
          </tbody>
        </table>

        <h4 style="color: #1b5e20; margin: 10px 0 4px 0;">አንቀጽ 2. የክፍያ ሁኔታ (Payment Terms)</h4>
        <div style="font-size: 11.5px; line-height: 1.6;">
          1. ገዥ የጠቅላላ ዋጋውን <strong>${advancePercent * 100}%</strong> ቅድመ ክፍያ (ብር <strong>${advanceAmount.toLocaleString()}</strong>) በባንክ ዝውውር ይፈጽማል።<br/>
          2. ቀሪውን <strong>${(1 - advancePercent) * 100}%</strong> ክፍያ (ብር <strong>${remainingAmount.toLocaleString()}</strong>) ዶሮዎችን በሚረከብበት ዕለት አጠናቆ ይከፍላል።
        </div>

        <h4 style="color: #1b5e20; margin: 10px 0 4px 0;">አንቀጽ 3. ነፃ የአገልግሎት ፓኬጆች (Free Package Offerings)</h4>
        <div style="font-size: 11.5px; line-height: 1.6;">
          ሻጭ ለገዥ የሚከተሉትን ነፃ አገልግሎቶች ያቀርባል፡-
          <ul style="margin: 4px 0 8px 20px;">
            ${freePackage.map(p => `<li><strong>${p}</strong></li>`).join('')}
          </ul>
        </div>

        <h4 style="color: #1b5e20; margin: 10px 0 4px 0;">አንቀጽ 4. የርክክብ ሁኔታ (Delivery Terms)</h4>
        <div style="font-size: 11.5px; line-height: 1.6;">
          ${contract.delivery_terms || 'ዶሮዎቹ በተስማሙበት ቀን እና ቦታ ጤንነታቸው ተረጋግጦ ይረከባሉ። ገዥ ከርክክብ በፊት አስፈላጊውን የማቆያ ዝግጅት ማጠናቀቅ አለበት።'}
        </div>

        <div style="display: flex; justify-content: space-between; margin-top: 24px; padding: 0 10px;">
          <div style="width: 45%;">
            <strong>የሻጭ ፊርማ (Seller):</strong><br/><br/>
            ስም፡ <strong>${seller.name}</strong><br/>
            ፊርማ፡ _______________________<br/>
            ቀን፡ ${contract.contract_date || new Date().toLocaleDateString('en-GB')}
          </div>
          <div style="width: 45%;">
            <strong>የገዥ ፊርማ (Buyer):</strong><br/><br/>
            ስም፡ <strong>${buyer.name}</strong><br/>
            ፊርማ፡ _______________________<br/>
            ቀን፡ ${contract.contract_date || new Date().toLocaleDateString('en-GB')}
          </div>
        </div>

        <h4 style="color: #374151; margin-top: 24px; margin-bottom: 6px;">የምስክሮች ስም እና ፊርማ (Witnesses)</h4>
        <table class="table-striped">
          <thead>
            <tr>
              <th style="width: 40px;" class="text-center">No</th>
              <th>የምስክር ሙሉ ስም</th>
              <th>አድራሻ / ስልክ</th>
              <th style="width: 140px;">ፊርማ</th>
              <th style="width: 100px;">ቀን</th>
            </tr>
          </thead>
          <tbody>
            ${(contract.witnesses || [1, 2, 3]).map((_, idx) => `
              <tr>
                <td class="text-center">${idx + 1}</td>
                <td style="height: 28px;"></td>
                <td></td>
                <td></td>
                <td></td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </body>
    </html>
  `;
}

/**
 * 11. Day-Old Chick Sale Contract Template (የእንቁላል ጣይ ጫጩት ውል New)
 */
export function renderDocSaleContractHtml(contract = {}) {
  const seller = contract.seller || 'Abunu Tekle & Friends Poultry Farm';
  const buyer = contract.buyer || {
    name: contract.customerName || 'Buyer Name',
    woreda: 'Woreda 03',
    house_no: '124',
    phone: contract.customerPhone || '09xxxxxxxx',
    tin: contract.customerTin || '-',
  };
  const quantity = contract.quantity || 2000;
  const unitPrice = contract.unit_price || 120;
  const subTotal = contract.sub_total || (quantity * unitPrice);
  const vat = contract.vat || (subTotal * 0.15);
  const grandTotal = contract.grand_total || (subTotal + vat);
  const advancePercent = contract.advance_percent || 0.8;
  const pickupDeadline = contract.pickup_deadline || 'Within 5 days of hatching notification';
  const consultancyUntil = contract.consultancy_until || 'Until birds finish production';

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>የእንቁላል ጣይ ጫጩት ውል New - DOC Sale Contract</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የእንቁላል ጣይ ጫጩት ውል New', 'Day-Old Chick (DOC) Sale Contract')}

        <div style="text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 12px; color: #1b5e20;">
          በአቡኑ ተክሌ እና ጓደኞቻቸው የዶሮ እርባታ እና በገዥ መካከል የተደረገ የአንድ ቀን ጫጩት ሽያጭ ውል
        </div>

        <div style="font-size: 11.5px; line-height: 1.6; margin-bottom: 12px;">
          ይህ ውል ቀን <strong>${contract.contract_date || new Date().toLocaleDateString('en-GB')}</strong> በ 
          <strong>${seller}</strong> (ከዚህ በኋላ "ሻጭ") እና በ 
          <strong>${buyer.name}</strong> (ወረዳ፡ ${buyer.woreda}፣ የቤት ቁጥር፡ ${buyer.house_no}፣ ስልክ፡ ${buyer.phone}፣ የግብር ከፋይ ቁጥር/TIN፡ ${buyer.tin}) (ከዚህ በኋላ "ገዥ") መካከል ተደረገ።
        </div>

        <table class="table-striped">
          <thead>
            <tr>
              <th>የጫጩት አይነት</th>
              <th class="text-right">ብዛት</th>
              <th class="text-right">የአንዱ ዋጋ</th>
              <th class="text-right">ድምር</th>
              <th class="text-right">ተ.እ.ታ (15%)</th>
              <th class="text-right">ጠቅላላ ዋጋ</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="font-bold">የእንቁላል ጣይ የአንድ ቀን ጫጩት (Bovans Brown DOC)</td>
              <td class="text-right">${quantity.toLocaleString()}</td>
              <td class="text-right">${unitPrice.toLocaleString()} ETB</td>
              <td class="text-right">${subTotal.toLocaleString()} ETB</td>
              <td class="text-right">${vat.toLocaleString()} ETB</td>
              <td class="text-right font-bold text-primary">${grandTotal.toLocaleString()} ETB</td>
            </tr>
          </tbody>
        </table>

        <div style="margin-top: 12px; font-size: 11.5px; line-height: 1.7;">
          <strong>የውል ቃሎችና ግዴታዎች፡-</strong><br/>
          1. <strong>ቅድመ ክፍያ፡</strong> ገዥ የጠቅላላ ዋጋውን <strong>${advancePercent * 100}%</strong> በሻጭ የኢትዮጵያ ንግድ ባንክ ሂሳብ ቁጥር <strong>1000799913376 (Abunu Tekle & Friends Poultry Farm)</strong> ያስገባል።<br/>
          2. <strong>የማንሻ ጊዜ (Pickup Deadline):</strong> ጫጩቶቹ እንደተፈለፈሉ ገዥ በ <strong>${pickupDeadline}</strong> ውስጥ መረከብ አለበት።<br/>
          3. <strong>የሙያ ድጋፍ (Consultancy):</strong> ሻጭ ጫጩቶቹ እስኪያድጉ እና ምርት እስኪያጠናቅቁ ድረስ (<strong>${consultancyUntil}</strong>) ነፃ የሙያ ክትትልና ድጋፍ ይሰጣል።
        </div>

        <div class="signature-grid" style="grid-template-columns: 1fr 1fr; margin-top: 40px;">
          <div class="signature-box">
            <strong>የሻጭ ፊርማ (Seller):</strong><br/><br/>
            <span>${seller}</span><br/>
            <span>ፊርማ፡ _____________________</span>
          </div>
          <div class="signature-box">
            <strong>የገዥ ፊርማ (Buyer):</strong><br/><br/>
            <span>${buyer.name}</span><br/>
            <span>ፊርማ፡ _____________________</span>
          </div>
        </div>
      </body>
    </html>
  `;
}

/**
 * 12. Broiler Replacement Contract Template (የሞቱ ምትክ)
 */
export function renderBroilerReplacementContractHtml(contract = {}) {
  const seller = contract.seller || 'TRUST Agro-Consulting PLC';
  const buyer = contract.buyer || {
    name: contract.customerName || 'Buyer Name',
    woreda: 'Woreda 05',
    house_no: '882',
    phone: contract.customerPhone || '09xxxxxxxx',
    tin: contract.customerTin || '-',
  };
  const birdType = contract.bird_type || 'Broiler (male meat chicken, 4 weeks)';
  const replacementCount = contract.replacement_count || 120;
  const deliveryDate = contract.delivery_date || 'Within 7 working days';
  const freePackage = contract.free_package_1500plus || [
    'Free debeaking',
    'Free vaccination',
    'Free consultancy',
    'Free transport',
    'Free chemical spray',
    'Road accident insurance',
  ];
  const loyaltyDiscount = contract.loyalty_discount || '10% discount on next Bovans Brown pullet purchase';

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>የሞቱ ምትክ ውል - Broiler Replacement</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የሞቱ ዶሮዎች ምትክ ውል', 'Broiler Mortality Replacement Agreement')}

        <div style="text-align: center; font-size: 14px; font-weight: bold; margin-bottom: 12px; color: #1b5e20;">
          የሞቱ ዶሮዎችን ለመተካት በሻጭ እና በገዥ መካከል የተደረገ የስምምነት ውል
        </div>

        <div style="font-size: 11.5px; line-height: 1.6; margin-bottom: 12px;">
          ይህ ስምምነት ቀን <strong>${contract.contract_date || new Date().toLocaleDateString('en-GB')}</strong> በ 
          <strong>${seller}</strong> እና በ <strong>${buyer.name}</strong> መካከል የተደረገ ሲሆን፣ ድርጅታችን ባለው የጥራት ዋስትና እና የደንበኞች አጋርነት መሠረት የሞቱ ዶሮዎችን በምትክ ለመስጠት ተስማምቷል።
        </div>

        <table class="table-striped">
          <thead>
            <tr>
              <th>የዶሮ አይነት (Bird Type)</th>
              <th class="text-right">የምትክ ብዛት (Replacement Count)</th>
              <th>የሚረከብበት ቀን (Delivery Date)</th>
              <th>ተፈጻሚነት (Validity)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="font-bold">${birdType}</td>
              <td class="text-right font-bold text-primary" style="font-size: 13px;">${replacementCount.toLocaleString()} ራስ</td>
              <td>${deliveryDate}</td>
              <td>${contract.validity || 'Until product is received'}</td>
            </tr>
          </tbody>
        </table>

        <div style="margin-top: 12px; font-size: 11.5px; line-height: 1.7;">
          <strong style="color: #e65100;">የ1500+ ነፃ አገልግሎት ጥቅል እና የታማኝነት ቅናሽ፡-</strong>
          <ul style="margin: 4px 0 8px 20px;">
            ${freePackage.map(p => `<li>${p}</li>`).join('')}
            <li><strong>የታማኝነት ጉርሻ፡</strong> ${loyaltyDiscount}</li>
          </ul>
        </div>

        <div class="signature-grid" style="grid-template-columns: 1fr 1fr; margin-top: 36px;">
          <div class="signature-box">
            <strong>የሻጭ ፊርማ (Seller):</strong><br/><br/>
            <span>${seller}</span><br/>
            <span>ፊርማ፡ _____________________</span>
          </div>
          <div class="signature-box">
            <strong>የገዥ ፊርማ (Buyer):</strong><br/><br/>
            <span>${buyer.name}</span><br/>
            <span>ፊርማ፡ _____________________</span>
          </div>
        </div>

        <h4 style="color: #374151; margin-top: 24px; margin-bottom: 6px;">የምስክሮች ስም እና ፊርማ (Witnesses)</h4>
        <table class="table-striped">
          <thead>
            <tr>
              <th style="width: 40px;" class="text-center">No</th>
              <th>የምስክር ሙሉ ስም</th>
              <th>ስልክ ቁጥር</th>
              <th style="width: 140px;">ፊርማ</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="text-center">1</td>
              <td style="height: 26px;"></td>
              <td></td>
              <td></td>
            </tr>
            <tr>
              <td class="text-center">2</td>
              <td style="height: 26px;"></td>
              <td></td>
              <td></td>
            </tr>
          </tbody>
        </table>
      </body>
    </html>
  `;
}

/**
 * 13. Feed Protocol Guide & Batch Calculator Template
 */
export function renderFeedProtocolGuideHtml(inputs = {}) {
  const birdsCount = Number(inputs.birdsCount || 1000);
  const dailyFeedKg = Number(inputs.dailyFeedKg || (birdsCount * 0.1)); // ~100g/bird
  
  // Bokashi standard: 4% bokashi + 96% standard feed
  const bokashiKg = dailyFeedKg * 0.04;
  const standardFeedKg = dailyFeedKg * 0.96;

  // Recipe batch calculation based on 40kg bran base
  const bokashiBatches = Math.max(1, Math.ceil(bokashiKg * 21 / 40)); // 21 days supply
  const emLiters = bokashiBatches * 2;
  const molassesKg = bokashiBatches * 2;
  const waterLiters = bokashiBatches * 16;
  const branKg = bokashiBatches * 40;

  return `
    <!DOCTYPE html>
    <html>
      <head>
        <title>Trust Agro - Feed Preparation Protocol</title>
        ${getBaseStyles()}
      </head>
      <body>
        ${getCompanyHeaderHtml('የቄብ እና የእንቁላል ጣይ መኖ ዝግጅት መመሪያ', 'Feed Preparation Protocol Guide')}

        <div style="background: #e8f5e9; border: 1px solid #c8e6c9; padding: 12px; border-radius: 6px; margin-bottom: 16px;">
          <h3 style="margin: 0 0 6px 0; color: #1b5e20;">1. Bokashi Fermentation Protocol</h3>
          <div style="font-size: 11.5px; line-height: 1.6;">
            <strong>Standard Formula:</strong> 2L EM + 2L Molasses + 16L Well Water + 40kg Wheat Bran.<br/>
            <strong>Fermentation Period:</strong> 21 days in airtight container / drums.<br/>
            <strong>Daily Feeding Ratio:</strong> <strong>4% Bokashi</strong> + <strong>96% Standard Feed</strong> mixed uniformly.
          </div>
        </div>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
          <div style="background: #fff3e0; border: 1px solid #ffe0b2; padding: 12px; border-radius: 6px;">
            <h4 style="margin: 0 0 6px 0; color: #e65100;">2. Acetic Acid Protocol</h4>
            <div style="font-size: 11px; line-height: 1.6;">
              • <strong>Dosage:</strong> 1ml per 1 Liter drinking water.<br/>
              • <strong>Schedule:</strong> 2x per week during hot season to maintain gut health and prevent bacterial overgrowth.
            </div>
          </div>

          <div style="background: #f3f4f6; border: 1px solid #e5e7eb; padding: 12px; border-radius: 6px;">
            <h4 style="margin: 0 0 6px 0; color: #374151;">3. EM Water Sanitization Protocol</h4>
            <div style="font-size: 11px; line-height: 1.6;">
              • <strong>Dosage:</strong> 1 Liter EM per 300 Liters well water.<br/>
              • <strong>Method:</strong> Blend with deep well water; closely monitor flock water and feed intake.
            </div>
          </div>
        </div>

        <div style="border: 1px solid #d1d5db; border-radius: 6px; padding: 12px; margin-bottom: 16px;">
          <h4 style="margin: 0 0 6px 0; color: #1b5e20;">4. Interactive Batch Recipe Calculator</h4>
          <div style="font-size: 11px; margin-bottom: 8px;">
            Calculated for <strong>${birdsCount.toLocaleString()}</strong> birds (${dailyFeedKg.toFixed(1)} kg daily total feed):
          </div>
          <table class="table-striped">
            <thead>
              <tr>
                <th>Ingredient / Component</th>
                <th class="text-right">Daily Requirement</th>
                <th class="text-right">21-Day Batch Preparation</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>Bokashi Probiotic Mixture (4%)</td>
                <td class="text-right font-bold text-primary">${bokashiKg.toFixed(2)} kg</td>
                <td class="text-right font-bold">${(bokashiKg * 21).toFixed(1)} kg</td>
              </tr>
              <tr>
                <td>Standard Concentrated Feed (96%)</td>
                <td class="text-right">${standardFeedKg.toFixed(2)} kg</td>
                <td class="text-right">${(standardFeedKg * 21).toFixed(1)} kg</td>
              </tr>
              <tr style="background: #fafafa;">
                <td colspan="3"><strong>Raw Materials Needed for 21-Day Fermentation (${bokashiBatches} batch units):</strong></td>
              </tr>
              <tr>
                <td>• Effective Microorganisms (EM)</td>
                <td class="text-right">—</td>
                <td class="text-right font-bold">${emLiters} Liters</td>
              </tr>
              <tr>
                <td>• Cane Molasses (ሞላሰስ)</td>
                <td class="text-right">—</td>
                <td class="text-right font-bold">${molassesKg} kg / Liters</td>
              </tr>
              <tr>
                <td>• Clean Well Water</td>
                <td class="text-right">—</td>
                <td class="text-right font-bold">${waterLiters} Liters</td>
              </tr>
              <tr>
                <td>• Wheat Bran (ፋጉሎ / መኖ)</td>
                <td class="text-right">—</td>
                <td class="text-right font-bold">${branKg} kg</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div style="background: #f9fafb; border: 1px solid #e5e7eb; padding: 10px; border-radius: 6px; font-size: 11px;">
          <strong style="color: #1b5e20;">Approved Fresh Greens Supplementation:</strong>
          <span style="margin-left: 8px;">Alfalfa (አልፋልፋ) • Espaniya (እስፓንያ) • Kale (ጎመን)</span>
        </div>
      </body>
    </html>
  `;
}
