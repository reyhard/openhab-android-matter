---
name: openhab-android-matter-mdns-ipv6-troubleshooting
description: Use when troubleshooting Thread IPv6 routing, OTBR reachability, OpenWRT/Raspberry Pi Avahi, stale _matterc._udp records, or mismatched Matter device IPs.
---

# mDNS and IPv6 Troubleshooting

Use this skill when the device was commissioned but openHAB cannot pair, when IP addresses do not match, or when Avahi/OpenWRT/OTBR/Thread routing is involved.

## Rules

- IPv6 reachability is required for Matter over Thread. IPv4 OTBR reachability is not enough.
- Do not invent Thread prefixes, interface names, or link-local next hops. Verify them from current commands.
- Compare the phone's connectedhomeip logs with Raspberry Pi/openHAB mDNS output before changing app code.
- Stale `_matterc._udp` records can point openHAB at an old device address even when the phone opened the current node correctly.

## Compare Phone And Avahi

From phone logs, extract:

- Node id.
- Operational IPv6 endpoint, usually `UDP:[<ipv6>]:5540`.
- OCW success and returned code/QR.

From Raspberry Pi/openHAB:

```bash
avahi-browse -rt _matterc._udp
```

Compare:

- Service name.
- Hostname.
- IPv6 address.
- Port.
- Discriminator TXT value.

If phone logs show one IPv6 address and Avahi shows another, treat mDNS/cache/advertisement state as suspect.

## Reachability Checks

Run pings from each relevant host:

```bash
ping -6 <current-device-ipv6>
ping -6 <stale-device-ipv6>
```

Use Windows PowerShell similarly when testing from the development machine:

```powershell
ping <device-ipv6>
```

If the stale address still responds, verify whether the device was actually factory reset or whether multiple records/devices exist.

## Avahi And Router Checks

On Raspberry Pi:

```bash
sudo systemctl restart avahi-daemon
avahi-browse -rt _matterc._udp
```

On OpenWRT:

```sh
/etc/init.d/avahi-daemon restart
```

If needed, restart networking or reboot the router only after confirming the stale record persists.

## IPv6 Routing Checks

- Confirm the Thread prefix and route exist on the host that runs openHAB.
- Confirm router advertisements are accepted when required by the environment.
- On Raspberry Pi, IPv6 RA route info prefix length settings may matter for Thread routes.
- On Windows, manual IPv6 routes may be needed in some setups, but route interface and next hop are environment-specific.

