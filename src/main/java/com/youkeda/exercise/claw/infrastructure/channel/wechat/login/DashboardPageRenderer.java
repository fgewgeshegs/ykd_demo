package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

final class DashboardPageRenderer {

    private DashboardPageRenderer() {
    }

    static String render() {
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>运行控制台 · ClawBot</title>
              <style>
                :root { color-scheme:dark; --bg:#070a0c; --panel:rgba(13,18,20,.94); --surface:#151a1d; --hover:#182024; --line:rgba(148,196,176,.12); --ink:#edf7f2; --secondary:#c5d3cc; --muted:#75857d; --dim:#47564f; --green:#29e38b; --cyan:#47cde8; --purple:#9a8cff; --red:#ff6f79; }
                * { box-sizing:border-box; }
                body { margin:0; min-height:100vh; color:var(--ink); background-color:var(--bg); background-image:linear-gradient(rgba(78,166,128,.035) 1px,transparent 1px),linear-gradient(90deg,rgba(78,166,128,.035) 1px,transparent 1px),radial-gradient(circle at 50% -20%,rgba(36,165,111,.09),transparent 42%); background-size:32px 32px,32px 32px,100% 100%; font-family:"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif; }
                .topbar { height:58px; border-bottom:1px solid var(--line); background:rgba(7,10,12,.88); backdrop-filter:blur(12px); }
                .topbar-inner { width:min(1120px,calc(100% - 48px)); height:100%; margin:auto; display:flex; align-items:center; justify-content:space-between; }
                .brand { display:flex; align-items:center; gap:10px; font-size:14px; font-weight:600; }
                .logo { position:relative; width:27px; height:27px; display:grid; place-items:center; border:1px solid rgba(41,227,139,.35); border-radius:7px; color:var(--green); background:rgba(41,227,139,.08); font-size:12px; box-shadow:inset 0 0 14px rgba(41,227,139,.08); }
                .connection { display:flex; align-items:center; gap:8px; color:#9ba0a8; font-size:12px; }
                .connection-dot { width:7px; height:7px; border-radius:50%; background:var(--green); box-shadow:0 0 0 4px rgba(41,227,139,.08),0 0 12px rgba(41,227,139,.55); animation:beacon 2s ease-in-out infinite; }
                main { width:min(1120px,calc(100% - 48px)); margin:auto; padding:34px 0 56px; }
                .page-head { display:flex; align-items:end; justify-content:space-between; margin-bottom:22px; }
                .overline { margin:0 0 8px; color:var(--green); font:10px ui-monospace,monospace; letter-spacing:.14em; }
                h1 { margin:0; font-size:28px; font-weight:560; letter-spacing:-.6px; }
                .page-subtitle { margin:7px 0 0; color:var(--muted); font-size:13px; }
                .head-signal { display:flex; align-items:flex-end; gap:14px; }
                .updated { color:var(--dim); font:10px ui-monospace,monospace; }
                .signal-matrix { height:28px; display:flex; align-items:flex-end; gap:3px; padding:5px 7px; border:1px solid var(--line); border-radius:6px; background:rgba(41,227,139,.025); }
                .signal-matrix i { width:2px; min-height:3px; border-radius:2px; background:var(--green); opacity:.38; box-shadow:0 0 6px rgba(41,227,139,.3); transition:height .45s ease,opacity .45s ease; }
                .summary { min-height:92px; display:grid; grid-template-columns:repeat(3,1fr); margin-bottom:16px; border:1px solid var(--line); border-radius:10px; background:var(--panel); box-shadow:inset 0 1px rgba(255,255,255,.025); overflow:hidden; }
                .metric { display:grid; grid-template-columns:1fr auto; grid-template-rows:auto auto; align-content:center; gap:7px 18px; padding:0 22px; }
                .metric + .metric { border-left:1px solid var(--line); }
                .metric-label { color:var(--muted); font-size:12px; }
                .metric-code { color:#43574e; font:9px ui-monospace,monospace; letter-spacing:.12em; }
                .metric-value { grid-row:1 / 3; grid-column:2; align-self:center; color:#f3fff9; font-size:27px; font-weight:560; font-variant-numeric:tabular-nums; letter-spacing:-.5px; text-shadow:0 0 18px rgba(41,227,139,.11); }
                .workspace { height:500px; display:grid; grid-template-columns:minmax(0,1fr) 286px; gap:16px; align-items:stretch; }
                .panel { border:1px solid var(--line); border-radius:10px; background-color:var(--panel); background-image:linear-gradient(rgba(71,205,232,.018) 1px,transparent 1px); background-size:100% 38px; box-shadow:inset 0 1px rgba(255,255,255,.02),0 16px 42px rgba(0,0,0,.12); overflow:hidden; }
                .workspace > .panel { min-height:0; display:flex; flex-direction:column; }
                .panel-head { min-height:52px; display:flex; align-items:center; justify-content:space-between; padding:0 16px; border-bottom:1px solid var(--line); background:rgba(10,14,16,.66); }
                .panel-title { margin:0; font-size:14px; font-weight:560; }
                .panel-title:before { content:""; display:inline-block; width:5px; height:5px; margin:0 9px 2px 0; border-radius:1px; background:var(--green); box-shadow:0 0 9px rgba(41,227,139,.55); }
                .filters { display:flex; gap:2px; padding:3px; border:1px solid var(--line); border-radius:7px; background:#0b0c0d; }
                .filter { min-height:28px; padding:0 10px; border:0; border-radius:5px; color:#676c74; background:transparent; font-size:12px; cursor:pointer; }
                .filter:hover { color:#b7bbc2; }
                .filter.active { color:var(--green); background:rgba(41,227,139,.08); box-shadow:inset 0 0 0 1px rgba(41,227,139,.1); }
                .timeline { min-height:0; flex:1; overflow-y:auto; overscroll-behavior:contain; scrollbar-width:thin; scrollbar-color:#34363b transparent; }
                .event { display:grid; grid-template-columns:68px 24px minmax(0,1fr) auto; gap:12px; align-items:start; padding:14px 16px; border-bottom:1px solid rgba(148,196,176,.075); transition:background .15s ease; }
                .event:hover { background:rgba(41,227,139,.025); }
                .event:last-child { border-bottom:0; }
                .time { padding-top:3px; color:#555a62; font:11px ui-monospace,monospace; }
                .event-icon { position:relative; width:24px; height:24px; display:grid; place-items:center; border:1px solid var(--line); border-radius:6px; color:#777c85; background:#101517; font:11px ui-monospace,monospace; }
                .event:not(:last-child) .event-icon:after { content:""; position:absolute; top:25px; left:11px; width:1px; height:42px; background:linear-gradient(var(--line),transparent); }
                .event.success .event-icon { color:var(--green); border-color:rgba(41,227,139,.22); background:rgba(41,227,139,.07); box-shadow:0 0 12px rgba(41,227,139,.06); }
                .event.failed .event-icon { color:#ee8989; border-color:rgba(224,93,93,.2); background:rgba(224,93,93,.07); }
                .event.signal-skill .event-icon { color:var(--cyan); border-color:rgba(71,205,232,.22); background:rgba(71,205,232,.07); }
                .event.signal-tool .event-icon { color:var(--purple); border-color:rgba(154,140,255,.22); background:rgba(154,140,255,.07); }
                .event-main { min-width:0; }
                .event-main strong { display:block; margin:1px 0 5px; color:#dfe1e5; font-size:13px; font-weight:560; }
                .event-main p { margin:0; color:#777c85; font-size:12px; line-height:1.55; }
                .tag { display:inline-flex; margin-right:6px; padding:2px 7px; border:1px solid var(--line); border-radius:4px; color:#a8b9b1; background:rgba(41,227,139,.025); font:11px ui-monospace,monospace; }
                .duration { padding-top:3px; color:#60656d; font:11px ui-monospace,monospace; }
                .empty { padding:110px 24px; color:#666b73; text-align:center; font-size:12px; }
                .side-column { min-height:0; display:grid; grid-template-rows:auto minmax(0,1fr); gap:16px; }
                .side-panel { min-height:0; }
                .side-panel:last-child { display:flex; flex-direction:column; }
                .side-body { padding:8px; }
                .status-row { min-height:44px; display:flex; align-items:center; justify-content:space-between; padding:0 9px; color:#9ba0a8; font-size:12px; }
                .status-row + .status-row { border-top:1px solid rgba(255,255,255,.045); }
                .status-value { display:flex; align-items:center; gap:7px; color:#c9cdd3; }
                .mini-dot { width:6px; height:6px; border-radius:50%; background:var(--green); box-shadow:0 0 8px rgba(41,227,139,.5); }
                .load-meter { width:58px; height:5px; overflow:hidden; border-radius:2px; background:#17201c; }
                .load-meter i { display:block; width:72%; height:100%; background:var(--green); box-shadow:0 0 8px rgba(41,227,139,.35); }
                .usage-list { min-height:0; display:grid; align-content:start; gap:2px; overflow-y:auto; overscroll-behavior:contain; scrollbar-width:thin; scrollbar-color:#34363b transparent; }
                .usage-item { min-height:42px; display:grid; grid-template-columns:minmax(0,1fr) 48px auto; gap:9px; align-items:center; padding:0 9px; border-radius:6px; color:#b8c8c0; font:11px ui-monospace,monospace; }
                .usage-item:hover { background:rgba(255,255,255,.025); }
                .usage-signal { height:3px; overflow:hidden; border-radius:2px; background:#18211d; }
                .usage-signal i { display:block; height:100%; background:var(--cyan); opacity:.72; box-shadow:0 0 6px rgba(71,205,232,.35); }
                .usage-count { min-width:24px; padding:2px 6px; border:1px solid var(--line); border-radius:999px; color:#737880; text-align:center; font-size:10px; }
                .side-empty { padding:18px 9px; color:#5f646c; font-size:11px; }
                .error-banner { margin-bottom:12px; padding:10px 12px; border:1px solid rgba(224,93,93,.18); border-radius:7px; color:#dc8383; background:rgba(224,93,93,.055); font-size:12px; }
                .timeline::-webkit-scrollbar,.usage-list::-webkit-scrollbar { width:7px; }
                .timeline::-webkit-scrollbar-thumb,.usage-list::-webkit-scrollbar-thumb { border:2px solid transparent; border-radius:999px; background:#34363b; background-clip:padding-box; }
                @keyframes beacon { 50% { opacity:.55; box-shadow:0 0 0 6px rgba(41,227,139,.04),0 0 6px rgba(41,227,139,.35); } }
                @media (max-width:900px) { .workspace { height:auto; grid-template-columns:1fr; } .workspace > .panel { height:520px; } .side-column { height:auto; grid-template-columns:1fr 1fr; grid-template-rows:1fr; } .side-panel:last-child { max-height:260px; } }
                @media (max-width:620px) { .topbar-inner,main { width:calc(100% - 28px); } main { padding-top:24px; } .page-head { align-items:start; } .updated { display:none; } .summary { grid-template-columns:1fr; } .metric { min-height:62px; } .metric + .metric { border-left:0; border-top:1px solid var(--line); } .event { grid-template-columns:22px minmax(0,1fr) auto; } .time { display:none; } .side-column { grid-template-columns:1fr; } .filters { overflow-x:auto; } }
              </style>
            </head>
            <body>
              <header class="topbar"><div class="topbar-inner"><div class="brand"><span class="logo">C</span><span>ClawBot</span><span style="color:#50545b;font-weight:400">/ 运行控制台</span></div><div class="connection"><span class="connection-dot"></span>微信已连接</div></div></header>
              <main>
                <div class="page-head"><div><p class="overline">SIGNAL MATRIX / LIVE TELEMETRY</p><h1>Agent 运行记录</h1><p class="page-subtitle">将每次请求、Skill 路由与工具调用映射为实时信号</p></div><div class="head-signal"><div class="updated" id="updated">WAITING FOR DATA</div><div class="signal-matrix" id="signalMatrix" aria-label="实时信号强度"><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div></div></div>
                <div id="errorBanner" class="error-banner" hidden></div>
                <section class="summary" aria-label="运行指标">
                  <div class="metric"><span class="metric-label">处理请求</span><span class="metric-code">REQUEST SIGNALS</span><strong class="metric-value" id="requests">0</strong></div>
                  <div class="metric"><span class="metric-label">工具调用</span><span class="metric-code">TOOL PULSES</span><strong class="metric-value" id="tools">0</strong></div>
                  <div class="metric"><span class="metric-label">执行失败</span><span class="metric-code">ERROR NODES</span><strong class="metric-value" id="failures">0</strong></div>
                </section>
                <div class="workspace">
                  <section class="panel">
                    <div class="panel-head"><h2 class="panel-title">活动时间线</h2><div class="filters"><button class="filter active" data-filter="ALL">全部</button><button class="filter" data-filter="SKILL">Skills</button><button class="filter" data-filter="TOOL">Tools</button></div></div>
                    <div class="timeline" id="timeline"><div class="empty">暂无活动记录</div></div>
                  </section>
                  <aside class="side-column">
                    <section class="panel side-panel"><div class="panel-head"><h2 class="panel-title">系统状态</h2></div><div class="side-body"><div class="status-row"><span>Agent</span><span class="status-value"><span class="mini-dot"></span>运行中</span></div><div class="status-row"><span>信号强度</span><span class="load-meter"><i></i></span></div><div class="status-row"><span>数据刷新</span><span class="status-value">2.5 s</span></div><div class="status-row"><span>存储</span><span class="status-value">SQLite</span></div></div></section>
                    <section class="panel side-panel"><div class="panel-head"><h2 class="panel-title">最近使用</h2></div><div class="side-body usage-list" id="usageList"><div class="side-empty">暂无 Skill 或 Tool 记录</div></div></section>
                  </aside>
                </div>
              </main>
              <script>
                let activities=[]; let activeFilter='ALL';
                const labels={REQUEST_RECEIVED:'收到请求',SKILL_SELECTED:'选择 Skill',TOOL_STARTED:'开始调用工具',TOOL_SUCCEEDED:'工具调用成功',TOOL_FAILED:'工具调用失败',TOOL_BLOCKED:'工具调用被阻止',RESPONSE_COMPLETED:'回复完成',REQUEST_FAILED:'请求失败'};
                function eventClass(item){const classes=[];if(item.status==='FAILED')classes.push('failed');else if(item.status==='SUCCESS')classes.push('success');if(item.eventType==='SKILL_SELECTED')classes.push('signal-skill');if(item.eventType.startsWith('TOOL_'))classes.push('signal-tool');return classes.join(' ');}
                function eventIcon(item){if(item.eventType==='SKILL_SELECTED')return'S';if(item.eventType.startsWith('TOOL_'))return'T';if(item.eventType==='RESPONSE_COMPLETED')return'✓';return'·';}
                function formatTime(value){return value?new Date(value).toLocaleTimeString('zh-CN',{hour12:false}):'—';}
                function visible(item){return activeFilter==='ALL'||(activeFilter==='SKILL'&&item.eventType==='SKILL_SELECTED')||(activeFilter==='TOOL'&&item.eventType.startsWith('TOOL_'));}
                function renderActivities(){
                  const root=document.getElementById('timeline');root.textContent='';const filtered=activities.filter(visible);
                  if(!filtered.length){const empty=document.createElement('div');empty.className='empty';empty.textContent='当前筛选条件下暂无活动';root.appendChild(empty);return;}
                  filtered.forEach(item=>{const row=document.createElement('article');row.className='event '+eventClass(item);const time=document.createElement('time');time.className='time';time.textContent=formatTime(item.createdAt);const icon=document.createElement('div');icon.className='event-icon';icon.textContent=eventIcon(item);const main=document.createElement('div');main.className='event-main';const title=document.createElement('strong');title.textContent=labels[item.eventType]||item.eventType;const detail=document.createElement('p');if(item.skillName){const tag=document.createElement('span');tag.className='tag';tag.textContent='skill:'+item.skillName;detail.appendChild(tag);}if(item.toolName){const tag=document.createElement('span');tag.className='tag';tag.textContent='tool:'+item.toolName;detail.appendChild(tag);}detail.appendChild(document.createTextNode(item.summary||''));main.append(title,detail);const duration=document.createElement('div');duration.className='duration';duration.textContent=item.durationMs==null?'':item.durationMs+' ms';row.append(time,icon,main,duration);root.appendChild(row);});
                }
                function renderUsage(){
                  const counts=new Map();activities.forEach(item=>{if(item.eventType==='SKILL_SELECTED'&&item.skillName)counts.set('Skill · '+item.skillName,(counts.get('Skill · '+item.skillName)||0)+1);if(item.eventType==='TOOL_SUCCEEDED'&&item.toolName)counts.set('Tool · '+item.toolName,(counts.get('Tool · '+item.toolName)||0)+1);});
                  const root=document.getElementById('usageList');root.textContent='';const entries=[...counts.entries()].slice(0,12);const max=Math.max(1,...entries.map(entry=>entry[1]));if(!entries.length){const empty=document.createElement('div');empty.className='side-empty';empty.textContent='暂无 Skill 或 Tool 记录';root.appendChild(empty);return;}entries.forEach(([name,count])=>{const row=document.createElement('div');row.className='usage-item';const label=document.createElement('span');label.textContent=name;const signal=document.createElement('span');signal.className='usage-signal';const level=document.createElement('i');level.style.width=Math.max(16,Math.round(count/max*100))+'%';signal.appendChild(level);const badge=document.createElement('span');badge.className='usage-count';badge.textContent=count;row.append(label,signal,badge);root.appendChild(row);});
                }
                function updateSignalMatrix(summary){const seed=summary.requestCount*3+summary.toolCallCount*5+summary.failureCount*11;document.querySelectorAll('#signalMatrix i').forEach((bar,index)=>{const level=4+((seed+index*7+Math.floor(Date.now()/2500))%17);bar.style.height=level+'px';bar.style.opacity=.28+level/30;});}
                async function refresh(){try{const[sr,ar]=await Promise.all([fetch('/api/dashboard/summary',{cache:'no-store'}),fetch('/api/activities?limit=80',{cache:'no-store'})]);if(!sr.ok||!ar.ok)throw new Error();const summary=await sr.json();activities=await ar.json();document.getElementById('requests').textContent=summary.requestCount;document.getElementById('tools').textContent=summary.toolCallCount;document.getElementById('failures').textContent=summary.failureCount;document.getElementById('updated').textContent='UPDATED '+new Date().toLocaleTimeString('zh-CN',{hour12:false});document.getElementById('errorBanner').hidden=true;updateSignalMatrix(summary);renderActivities();renderUsage();}catch(e){const banner=document.getElementById('errorBanner');banner.hidden=false;banner.textContent='控制台数据暂时不可用，正在自动重试';}}
                document.querySelectorAll('.filter').forEach(button=>button.addEventListener('click',()=>{document.querySelectorAll('.filter').forEach(x=>x.classList.remove('active'));button.classList.add('active');activeFilter=button.dataset.filter;renderActivities();}));
                refresh();setInterval(refresh,2500);
              </script>
            </body>
            </html>
            """;
    }
}