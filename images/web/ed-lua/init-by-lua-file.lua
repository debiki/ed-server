ngx.log(ngx.INFO, 'Running: init_by_lua_file')


-- Bandwidth limiting
--
-- Pre-load Lua code — so the forked workers won''t need to load it themselves,
-- saving some memory.
-- See: https://github.com/openresty/lua-nginx-module#init_by_lua
--
require("lua-limit-bandwidth/access-phase")
require("lua-limit-bandwidth/log-phase")


-- Auto HTTPS
--
-- Find default config here:
-- ./openresty-pkgs/usr-local-openresty-site/lualib/resty/acme/autossl.lua
--
require("resty.acme.autossl").init({
    -- If one has accepted the LetsEncrypt ToS, https://letsencrypt.org/repository/.
    -- SHOULD be configurable so self-hosted admins need to edit & change to true?
    tos_accepted = true,

    -- Set to true in test env, to use LetsEncrypt staging env, and avoid
    -- "Too many requests" errors when using LetsEncrypt for real.
    -- staging = true,

    -- By default only RSA (not ECC) enabled.
    -- domain_key_types = { 'rsa', 'ecc' },

    -- By default only challenges over HTTP (not HTTPS, tls-alpn-01) enabled.
    -- In lua-resty-acme, tls-alpn-01 is experimental. So not incl here.
    -- If enabling, remember to also update:
    --    conf/sites-enabled-manual/talkyard-servers.conf,
    -- enabled_challenge_handlers = { 'http-01', 'tls-alpn-01' },

    -- LetsEncrypt''s rate limits are higher, if one specifies an account key.
    account_key_path = "/etc/nginx/acme-account.key",

    -- SHOULD be configurable, default empty.
    -- ${YOUR_SECURITY_EMAIL_ADDR}  in  .env  ?
    account_email = "security@talkyard.io",

    domain_whitelist_callback = function(domain)
        ngx.log(ngx.DEBUG, "Checking if should gen cert for: " .. domain
              .. " [TyMGENCRTCK]")

        -- This might work for avoiding trying to get certs for IP addrs
        -- (which isn't possible, just causes some pointless netw traffic).
        -- But how generate a request with an IP addr as hostname?
        -- Surprisinly, none of this worked: (where 1.2.3.4 is the server's ip addr)
        --  curl -k -v -v                    'https://1.2.3.4/test'
        --  curl -k -v -v -H 'Host: 1.2.3.4' 'https://1.2.3.4/test'
        --  curl -k -v -v -H 'Host: 1.2.3.4' 'https://talkyard.io/test'
        --  curl -k -v -v -H 'Host: 1.2.3.4' 'https://funny.co/test' \
        --                                --resolve 'funny.co:443:1.2.3.4'
        --
        -- local ipv4Parts = { domain:match("^(%d+)%.(%d+)%.(%d+)%.(%d+)$") }
        -- if #ipv4Parts == 4 then
        --   ngx.log(ngx.INFO, "Checkinp if is IP addr: " .. domain
        --         .. " [TyMGENCRTMBYIP]")
        --   local isIp = true
        --   for _, v in pairs(ipv4Parts) do
        --     if tonumber(v) > 255 then
        --       isIp = false
        --     end
        --   end
        --   if isIp then
        --     ngx.log(ngx.INFO, "Should not gen cert for IP addr: " .. domain
        --           .. " [TyMGENCRTNOIP4]")
        --     return false
        --   end
        -- end
        -- Could also check if is IPv6, seee:
        -- https://stackoverflow.com/a/16643628

        local http = require('resty.http')
        local httpc = http.new()
        httpc:set_timeouts(1000, 5000, 5000)
        httpc:connect('app', 9000)

        local res, err = httpc:request({
            path = '/-/_int_req/hostname-should-have-cert',
            headers = {
                -- ['X-Request-Id'] = ngx.var.request_id,  — cannot access here
                ['Host'] = domain,
            },
        })

        if not res then
            ngx.log(ngx.INFO, "Error checking if should gen cert for: " .. domain
                  .. ", err: " .. err .. " [TyMGENCRTERR]")
            return false
        end

        if res.status ~= 200 then
            ngx.log(ngx.INFO, "Should not gen cert for: " .. domain .. " [TyMGENCRTNO]")
            return false
        end

        ngx.log(ngx.INFO, "Should gen cert for: " .. domain .. " [TyMGENCRTYES]")
        return true
    end,

    storage_adapter = 'redis',
    storage_config = {
        host = 'cache',  -- the service name in docker-compose.yml
        port = 6379,     -- the default Redis port
        database = 0,    -- Ty uses just one Redis database
        auth = nil,      -- no password, Redis not publ accessible
    },
})
