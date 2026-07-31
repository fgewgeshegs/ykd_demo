package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

final class LoginPageRenderer {

    private LoginPageRenderer() {
    }

    static String render(String qrUrl, ObjectMapper objectMapper) throws JsonProcessingException {
        String qrJson = objectMapper.writeValueAsString(qrUrl == null ? "" : qrUrl);
        return """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>微信登录 · ClawBot</title>
              <style>
                :root { color-scheme:dark; --bg:#08090a; --panel:#0f1011; --surface:#161719; --line:rgba(255,255,255,.08); --ink:#f7f8f8; --muted:#8a8f98; --green:#18b66a; }
                * { box-sizing:border-box; }
                body { margin:0; min-height:100vh; color:var(--ink); background:var(--bg); font-family:"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif; }
                .page { min-height:100vh; display:grid; grid-template-rows:1fr 48px; }
                main { width:min(920px,calc(100% - 48px)); margin:auto; display:grid; grid-template-columns:minmax(0,1fr) 392px; align-items:center; gap:72px; padding:40px 0 64px; }
                .intro { align-self:center; }
                .eyebrow { margin:0 0 18px; color:#74d99e; font-size:12px; font-weight:600; letter-spacing:.08em; text-transform:uppercase; }
                h1 { max-width:480px; margin:0; font-size:42px; line-height:1.14; font-weight:560; letter-spacing:-1.2px; }
                .intro-copy { max-width:430px; margin:18px 0 30px; color:var(--muted); font-size:15px; line-height:1.8; }
                .steps { display:grid; gap:16px; }
                .step { display:grid; grid-template-columns:28px 1fr; gap:12px; align-items:start; color:#d0d6e0; font-size:13px; }
                .step-num { width:24px; height:24px; display:grid; place-items:center; border:1px solid var(--line); border-radius:6px; color:#9ea4ad; background:rgba(255,255,255,.03); font:11px ui-monospace,monospace; }
                .step small { display:block; margin-top:3px; color:#686d75; font-size:11px; }
                .login-card { padding:30px; border:1px solid var(--line); border-radius:14px; background:var(--panel); box-shadow:0 24px 80px rgba(0,0,0,.34); }
                .card-head { margin-bottom:22px; }
                .card-head h2 { margin:0; font-size:18px; font-weight:560; letter-spacing:-.25px; }
                .card-head p { margin:7px 0 0; color:var(--muted); font-size:12px; }
                .qr-shell { position:relative; width:252px; height:252px; margin:auto; display:grid; place-items:center; padding:16px; border-radius:10px; background:#fff; }
                #qrcode img, #qrcode canvas { display:block; width:220px!important; height:220px!important; }
                .loading { color:#6f747b; font-size:12px; }
                .status { display:flex; align-items:center; justify-content:center; gap:8px; margin-top:20px; color:#d8dce2; font-size:13px; font-weight:520; }
                .dot { width:7px; height:7px; border-radius:50%; background:#e4a11b; box-shadow:0 0 0 4px rgba(228,161,27,.1); animation:pulse 1.5s ease-in-out infinite; }
                .status-sub { min-height:18px; margin:7px 0 0; color:#70757e; text-align:center; font-size:11px; }
                .privacy { margin:22px 0 0; padding-top:18px; border-top:1px solid var(--line); color:#62666d; text-align:center; font-size:10px; }
                .error { margin-top:14px; padding:9px 10px; border:1px solid rgba(220,80,80,.22); border-radius:7px; color:#e49191; background:rgba(220,80,80,.07); text-align:center; font-size:11px; }
                footer { width:min(1120px,calc(100% - 48px)); margin:auto; color:#4f5359; font-size:10px; }
                @keyframes pulse { 50% { opacity:.45; transform:scale(.82); } }
                @media (prefers-reduced-motion:reduce) { .dot { animation:none; } }
                @media (max-width:820px) { .page { grid-template-rows:1fr 36px; } main { width:min(440px,calc(100% - 32px)); grid-template-columns:1fr; gap:30px; padding:28px 0; } .intro { text-align:center; } h1 { font-size:30px; } .intro-copy { margin:12px auto 0; } .steps { display:none; } .login-card { padding:24px; } footer { width:calc(100% - 32px); } }
                @media (max-width:390px) { .qr-shell { width:224px; height:224px; padding:12px; } #qrcode img,#qrcode canvas { width:200px!important; height:200px!important; } }
              </style>
            </head>
            <body>
              <div class="page">
                <main>
                  <section class="intro">
                    <p class="eyebrow">Local assistant</p>
                    <h1>连接微信，开始使用你的智能助手</h1>
                    <p class="intro-copy">完成扫码后将自动进入运行控制台。你可以在那里查看每次请求选择了什么 Skill，以及调用了哪些工具。</p>
                    <div class="steps">
                      <div class="step"><span class="step-num">01</span><span>打开微信扫一扫<small>扫描右侧二维码</small></span></div>
                      <div class="step"><span class="step-num">02</span><span>在手机上确认<small>等待连接状态验证</small></span></div>
                      <div class="step"><span class="step-num">03</span><span>自动进入控制台<small>查看 Agent 实时活动</small></span></div>
                    </div>
                  </section>
                  <section class="login-card" aria-labelledby="loginTitle">
                    <div class="card-head"><h2 id="loginTitle">微信扫码登录</h2><p>二维码将在两分钟后失效</p></div>
                    <div class="qr-shell"><div id="qrcode" class="loading">正在生成二维码…</div></div>
                    <div class="status" aria-live="polite"><span class="dot"></span><span id="statusText">等待扫码</span></div>
                    <p class="status-sub" id="statusSub">请使用微信扫描二维码</p>
                    <p class="privacy">登录凭证仅保存在当前设备</p>
                    <div class="error" id="error" hidden></div>
                  </section>
                </main>
                <footer>ClawBot · Local Control</footer>
              </div>
              <script>
                const QR_URL = __QR_JSON__;
                const box = document.getElementById('qrcode');
                function showError(message) { const error=document.getElementById('error'); error.hidden=false; error.textContent=message; }
                function loadQr() {
                  if (!QR_URL) { showError('暂未获取到二维码，请稍候刷新页面'); return; }
                  const script=document.createElement('script');
                  script.src='https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js';
                  script.onload=()=>{ box.textContent=''; box.className=''; new QRCode(box,{text:QR_URL,width:220,height:220,colorDark:'#101316',colorLight:'#ffffff',correctLevel:QRCode.CorrectLevel.M}); };
                  script.onerror=()=>showError('二维码组件加载失败，请检查网络后刷新');
                  document.head.appendChild(script);
                }
                async function poll() {
                  try {
                    const response=await fetch('/login/status',{cache:'no-store'}); const data=await response.json();
                    const text=document.getElementById('statusText'); const sub=document.getElementById('statusSub');
                    if(data.status==='SUCCESS'){ text.textContent='连接成功'; sub.textContent='正在进入控制台…'; location.replace('/dashboard'); return; }
                    if(data.status==='SCANNED'){ text.textContent='正在验证'; sub.textContent='请在手机上确认登录'; }
                    if(data.status==='FAILED'||data.status==='TIMEOUT'){ text.textContent='二维码已失效'; sub.textContent='请重启应用获取新二维码'; showError('本次连接未完成'); return; }
                  } catch(e){ document.getElementById('statusSub').textContent='本地服务连接中断，正在重试…'; }
                  setTimeout(poll,1500);
                }
                loadQr(); poll();
              </script>
            </body>
            </html>
            """.replace("__QR_JSON__", qrJson);
    }
}