-- Maintenance note:
-- disconnectNode() must stay identical across put.lua, get.lua, and remove.lua.
-- setWarmest() must stay identical across put.lua and get.lua.
-- Redis Lua has no built-in import/include across separate script files, so
-- these shared helpers are duplicated and must be kept in sync manually.
--
-- Helper functions
local function disconnectNode(nodeKey)
    local prevKey = redis.call('HGET', KEYS[2], nodeKey)
    local nextKey = redis.call('HGET', KEYS[3], nodeKey)

    if prevKey then
        if nextKey then
            redis.call('HSET', KEYS[3], prevKey, nextKey)
        else
            redis.call('HDEL', KEYS[3], prevKey)
        end
    end

    if nextKey then
        if prevKey then
            redis.call('HSET', KEYS[2], nextKey, prevKey)
        else
            redis.call('HDEL', KEYS[2], nextKey)
        end
    end

    redis.call('HDEL', KEYS[2], nodeKey)
    redis.call('HDEL', KEYS[3], nodeKey)
end

local function setWarmest(nodeKey)
    local currentWarmest = redis.call('HGET', KEYS[4], 'warmest')

    if not currentWarmest then
        redis.call('HSET', KEYS[4], 'warmest', nodeKey)
    elseif currentWarmest ~= nodeKey then
        disconnectNode(nodeKey)
        redis.call('HSET', KEYS[2], nodeKey, currentWarmest)
        redis.call('HSET', KEYS[3], currentWarmest, nodeKey)
        redis.call('HSET', KEYS[4], 'warmest', nodeKey)
    end
end

-- Main flow
local key = ARGV[1]
local value = redis.call('HGET', KEYS[1], key)

if not value then
    return nil
end

setWarmest(key)
return value
