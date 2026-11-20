import React, { useState, useEffect, useRef, useCallback } from 'react';
import { 
  MapPin, Search, Navigation, X, Check, Loader2, 
  Home, Briefcase, Tag, AlertCircle, Compass 
} from 'lucide-react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { LucianButton } from './ui/LucianPrimitives';

// Tọa độ mặc định: Trung tâm Hà Nội (21.0285, 105.8542) theo yêu cầu
const DEFAULT_CENTER = {
  lat: 21.028511,
  lng: 105.854201,
  zoom: 15,
};

// Custom Marker Pin với thiết kế QuickShop Mall sắc nét
const customPinIcon = L.divIcon({
  className: 'quickshop-map-pin',
  html: `
    <div style="position: relative; display: flex; flex-direction: column; align-items: center; cursor: grab;">
      <div style="width: 38px; height: 38px; border-radius: 50%; background: linear-gradient(135deg, #f97316 0%, #ea580c 100%); display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 25px -5px rgba(234, 88, 12, 0.5), 0 8px 10px -6px rgba(0, 0, 0, 0.1); border: 3px solid #ffffff; transition: transform 0.2s ease;">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ffffff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/>
          <circle cx="12" cy="10" r="3"/>
        </svg>
      </div>
      <div style="width: 12px; height: 5px; background: rgba(0,0,0,0.25); border-radius: 50%; filter: blur(1px); margin-top: 2px;"></div>
    </div>
  `,
  iconSize: [38, 45],
  iconAnchor: [19, 43],
  popupAnchor: [0, -42],
});

export default function AddressMapPickerModal({
  isOpen,
  onClose,
  onSave,
  initialData = null,
  title = 'Thêm / Sửa Địa Chỉ Giao Hàng',
}) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const reverseDebounceTimerRef = useRef(null);
  const searchDebounceTimerRef = useRef(null);

  // Form states
  const [recipientName, setRecipientName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [streetAddress, setStreetAddress] = useState('');
  const [ward, setWard] = useState('');
  const [district, setDistrict] = useState('');
  const [city, setCity] = useState('Hà Nội');
  const [fullAddress, setFullAddress] = useState('');
  const [tag, setTag] = useState('Nhà riêng');
  const [isDefault, setIsDefault] = useState(false);

  // Coordinates states
  const [lat, setLat] = useState(DEFAULT_CENTER.lat);
  const [lng, setLng] = useState(DEFAULT_CENTER.lng);

  // Search states
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);

  // Status flags
  const [isReverseGeocoding, setIsReverseGeocoding] = useState(false);
  const [isGettingLocation, setIsGettingLocation] = useState(false);
  const [geoError, setGeoError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Đồng bộ form khi initialData thay đổi
  useEffect(() => {
    if (initialData) {
      setRecipientName(initialData.recipientName || initialData.name || '');
      setPhoneNumber(initialData.phoneNumber || initialData.phone || '');
      setStreetAddress(initialData.streetAddress || initialData.address || initialData.addressLine || '');
      setWard(initialData.ward || '');
      setDistrict(initialData.district || '');
      setCity(initialData.city || 'Hà Nội');
      setFullAddress(initialData.fullAddress || '');
      setTag(initialData.tag || 'Nhà riêng');
      setIsDefault(Boolean(initialData.isDefault));

      const parsedLat = Number(initialData.latitude);
      const parsedLng = Number(initialData.longitude);
      if (!isNaN(parsedLat) && parsedLat !== 0 && !isNaN(parsedLng) && parsedLng !== 0) {
        setLat(parsedLat);
        setLng(parsedLng);
      } else {
        setLat(DEFAULT_CENTER.lat);
        setLng(DEFAULT_CENTER.lng);
      }
    } else {
      setRecipientName('');
      setPhoneNumber('');
      setStreetAddress('');
      setWard('');
      setDistrict('');
      setCity('Hà Nội');
      setFullAddress('');
      setTag('Nhà riêng');
      setIsDefault(false);
      setLat(DEFAULT_CENTER.lat);
      setLng(DEFAULT_CENTER.lng);
    }
    setGeoError(null);
    setSearchQuery('');
    setSearchResults([]);
  }, [initialData, isOpen]);

  // Reverse Geocoding qua Nominatim OpenStreetMap (Debounced 300ms)
  const performReverseGeocode = useCallback((latitude, longitude) => {
    if (reverseDebounceTimerRef.current) {
      clearTimeout(reverseDebounceTimerRef.current);
    }

    setIsReverseGeocoding(true);
    reverseDebounceTimerRef.current = setTimeout(async () => {
      try {
        const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&accept-language=vi`;
        const res = await fetch(url, {
          headers: {
            'User-Agent': 'QuickShop-ECommerce-Platform/1.0',
          },
        });
        if (!res.ok) throw new Error('Không thể tải dữ liệu địa chỉ');
        const data = await res.json();

        if (data && data.address) {
          const addr = data.address;
          const road = addr.road || addr.pedestrian || addr.suburb || '';
          const houseNumber = addr.house_number ? `${addr.house_number} ` : '';
          const detectedStreet = (houseNumber + road).trim();

          const detectedWard = addr.quarter || addr.suburb || addr.neighbourhood || addr.village || '';
          const detectedDistrict = addr.city_district || addr.district || addr.county || addr.town || '';
          const detectedCity = addr.city || addr.state || addr.province || 'Hà Nội';

          if (detectedStreet) setStreetAddress(detectedStreet);
          if (detectedWard) setWard(detectedWard);
          if (detectedDistrict) setDistrict(detectedDistrict);
          if (detectedCity) setCity(detectedCity);

          const formatted = data.display_name || '';
          setFullAddress(formatted);

          if (markerRef.current) {
            markerRef.current.bindPopup(
              `<div style="font-size: 11px; font-weight: 600; line-height: 1.4; color: #1e293b;">
                📍 <strong>Vị trí đã chọn:</strong><br/>${formatted}
              </div>`
            ).openPopup();
          }
        }
      } catch (err) {
        console.warn('Lỗi Reverse Geocoding:', err);
      } finally {
        setIsReverseGeocoding(false);
      }
    }, 300);
  }, []);

  // Tìm kiếm địa chỉ qua Nominatim Search (Debounced 500ms)
  const handleSearchInputChange = (e) => {
    const query = e.target.value;
    setSearchQuery(query);

    if (searchDebounceTimerRef.current) {
      clearTimeout(searchDebounceTimerRef.current);
    }

    if (!query || query.trim().length < 2) {
      setSearchResults([]);
      setShowDropdown(false);
      setIsSearching(false);
      return;
    }

    setIsSearching(true);
    searchDebounceTimerRef.current = setTimeout(async () => {
      try {
        const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=vn&limit=5&accept-language=vi&q=${encodeURIComponent(query.trim())}`;
        const res = await fetch(url, {
          headers: {
            'User-Agent': 'QuickShop-ECommerce-Platform/1.0',
          },
        });
        if (!res.ok) throw new Error('Lỗi tìm kiếm');
        const list = await res.json();
        setSearchResults(list || []);
        setShowDropdown(true);
      } catch (err) {
        console.warn('Lỗi tìm kiếm địa chỉ:', err);
        setSearchResults([]);
      } finally {
        setIsSearching(false);
      }
    }, 500);
  };

  // Chọn 1 kết quả từ danh sách tìm kiếm
  const handleSelectSearchResult = (item) => {
    const newLat = parseFloat(item.lat);
    const newLng = parseFloat(item.lon);

    setLat(newLat);
    setLng(newLng);
    setSearchQuery(item.display_name);
    setShowDropdown(false);

    if (mapInstanceRef.current) {
      mapInstanceRef.current.setView([newLat, newLng], 16, { animate: true });
    }
    if (markerRef.current) {
      markerRef.current.setLatLng([newLat, newLng]);
    }

    performReverseGeocode(newLat, newLng);
  };

  // Lấy vị trí hiện tại (GPS Geolocation)
  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) {
      setGeoError('Trình duyệt của bạn không hỗ trợ định vị GPS');
      return;
    }

    setIsGettingLocation(true);
    setGeoError(null);

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const currentLat = pos.coords.latitude;
        const currentLng = pos.coords.longitude;

        setLat(currentLat);
        setLng(currentLng);
        setIsGettingLocation(false);

        if (mapInstanceRef.current) {
          mapInstanceRef.current.setView([currentLat, currentLng], 16, { animate: true });
        }
        if (markerRef.current) {
          markerRef.current.setLatLng([currentLat, currentLng]);
        }

        performReverseGeocode(currentLat, currentLng);
      },
      (err) => {
        setIsGettingLocation(false);
        console.warn('Geolocation error:', err);
        if (err.code === 1) {
          setGeoError('Bạn đã từ chối quyền truy cập vị trí. Hãy bật GPS trên trình duyệt để định vị nhanh.');
        } else {
          setGeoError('Không thể xác định vị trí hiện tại của thiết bị.');
        }
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 }
    );
  };

  // Khởi tạo và quản lý vòng đời của Leaflet Map
  useEffect(() => {
    if (!isOpen) {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
      return;
    }

    const timer = setTimeout(() => {
      if (!mapContainerRef.current) return;

      if (!mapInstanceRef.current) {
        const map = L.map(mapContainerRef.current, {
          center: [lat, lng],
          zoom: DEFAULT_CENTER.zoom,
          zoomControl: false,
        });

        // Thêm zoom control ở góc phải dưới
        L.control.zoom({ position: 'bottomright' }).addTo(map);

        // Tile layer chuẩn OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; OpenStreetMap contributors',
        }).addTo(map);

        // Marker có thể kéo thả draggable: true
        const marker = L.marker([lat, lng], {
          icon: customPinIcon,
          draggable: true,
        }).addTo(map);

        // Khi kéo thả marker xong: cập nhật lat/lng và reverse geocode
        marker.on('dragend', (e) => {
          const position = e.target.getLatLng();
          const targetLat = position.lat;
          const targetLng = position.lng;
          setLat(targetLat);
          setLng(targetLng);
          performReverseGeocode(targetLat, targetLng);
        });

        // Khi click trực tiếp lên bản đồ: di chuyển marker và reverse geocode
        map.on('click', (e) => {
          const clickedLat = e.latlng.lat;
          const clickedLng = e.latlng.lng;
          marker.setLatLng([clickedLat, clickedLng]);
          setLat(clickedLat);
          setLng(clickedLng);
          performReverseGeocode(clickedLat, clickedLng);
        });

        mapInstanceRef.current = map;
        markerRef.current = marker;

        // Bắt buộc gọi invalidateSize() ngay sau khi container hiển thị để tránh xám tile
        map.invalidateSize();

        // Reverse geocode vị trí khởi tạo nếu chưa có fullAddress
        if (!initialData?.fullAddress) {
          performReverseGeocode(lat, lng);
        }
      } else {
        mapInstanceRef.current.setView([lat, lng], DEFAULT_CENTER.zoom);
        if (markerRef.current) {
          markerRef.current.setLatLng([lat, lng]);
        }
        mapInstanceRef.current.invalidateSize();
      }
    }, 150);

    return () => {
      clearTimeout(timer);
    };
  }, [isOpen, lat, lng, performReverseGeocode, initialData]);

  // Handle Form Submit
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!recipientName.trim()) {
      alert('Vui lòng nhập tên người nhận hàng');
      return;
    }
    if (!phoneNumber.trim()) {
      alert('Vui lòng nhập số điện thoại người nhận');
      return;
    }
    if (!streetAddress.trim()) {
      alert('Vui lòng nhập số nhà, tên đường hoặc chọn vị trí trên bản đồ');
      return;
    }

    setIsSubmitting(true);
    try {
      const computedFullAddress = fullAddress.trim() || 
        `${streetAddress.trim()}, ${ward.trim() ? ward.trim() + ', ' : ''}${district.trim() ? district.trim() + ', ' : ''}${city.trim()}`.replace(/,\s*,/g, ',').trim();

      const addressPayload = {
        recipientName: recipientName.trim(),
        phoneNumber: phoneNumber.trim(),
        streetAddress: streetAddress.trim(),
        ward: ward.trim() || 'Phường Trung Tâm',
        district: district.trim() || 'Quận Trung Tâm',
        city: city.trim() || 'Hà Nội',
        fullAddress: computedFullAddress,
        tag: tag || 'Nhà riêng',
        isDefault: Boolean(isDefault),
        latitude: lat ? parseFloat(lat.toFixed(6)) : null,
        longitude: lng ? parseFloat(lng.toFixed(6)) : null,
      };

      if (onSave) {
        await onSave(addressPayload, initialData?.id);
      }
      onClose();
    } catch (err) {
      console.error('Lỗi lưu địa chỉ:', err);
      alert('Không thể lưu địa chỉ. Vui lòng kiểm tra lại thông tin!');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-3 sm:p-4 overflow-y-auto bg-black/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div 
        className="bg-white rounded-3xl max-w-3xl w-full shadow-2xl border border-slate-200/90 overflow-hidden flex flex-col max-h-[92vh] animate-in zoom-in-95 duration-200"
        onClick={(e) => {
          if (showDropdown) setShowDropdown(false);
        }}
      >
        {/* Header Modal */}
        <div className="p-4 sm:p-5 bg-gradient-to-r from-orange-500 via-rose-500 to-amber-500 text-white flex items-center justify-between shadow-md shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center font-bold text-lg">
              <MapPin className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="font-black text-sm sm:text-base text-white">{title}</h3>
              <p className="text-[11px] text-orange-100 flex items-center gap-1">
                <span>Chọn trực tiếp trên bản đồ OpenStreetMap (Miễn phí)</span>
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/20 hover:bg-white/30 text-white flex items-center justify-center transition cursor-pointer"
            title="Đóng modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div className="p-4 sm:p-6 overflow-y-auto space-y-4 text-xs">
          
          {/* Top Bar: Search Input & Geolocation Button */}
          <div className="relative space-y-2">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={handleSearchInputChange}
                  onFocus={() => {
                    if (searchResults.length > 0) setShowDropdown(true);
                  }}
                  placeholder="Gõ tìm kiếm số nhà, tên đường, tòa nhà hoặc địa danh..."
                  className="w-full pl-10 pr-9 py-2.5 bg-slate-50 border border-slate-300 rounded-2xl text-xs text-slate-900 focus:outline-none focus:border-orange-500 focus:bg-white transition"
                />
                {isSearching && (
                  <Loader2 className="w-4 h-4 text-orange-500 animate-spin absolute right-3 top-1/2 -translate-y-1/2" />
                )}
                {searchQuery && !isSearching && (
                  <button
                    type="button"
                    onClick={() => {
                      setSearchQuery('');
                      setSearchResults([]);
                      setShowDropdown(false);
                    }}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>

              {/* Button: Lấy vị trí hiện tại (GPS) */}
              <button
                type="button"
                onClick={handleGetCurrentLocation}
                disabled={isGettingLocation}
                className="px-3.5 py-2.5 bg-orange-50 hover:bg-orange-100 text-orange-700 border border-orange-200 rounded-2xl font-bold flex items-center gap-1.5 transition active:scale-95 shrink-0 cursor-pointer disabled:opacity-50"
                title="Bấm để tự động định vị vị trí GPS hiện tại của bạn"
              >
                <Navigation className={`w-3.5 h-3.5 ${isGettingLocation ? 'animate-spin' : ''}`} />
                <span className="hidden sm:inline">Vị trí hiện tại</span>
              </button>
            </div>

            {/* Nominatim Search Dropdown Suggestions */}
            {showDropdown && searchResults.length > 0 && (
              <div className="absolute left-0 right-0 top-full mt-1 bg-white rounded-2xl shadow-xl border border-slate-200 z-50 overflow-hidden divide-y divide-slate-100 max-h-56 overflow-y-auto">
                {searchResults.map((item, idx) => (
                  <div
                    key={idx}
                    onClick={() => handleSelectSearchResult(item)}
                    className="p-3 hover:bg-orange-50/80 cursor-pointer transition flex items-start gap-2.5 text-left"
                  >
                    <Compass className="w-4 h-4 text-orange-600 shrink-0 mt-0.5" />
                    <div>
                      <div className="font-bold text-slate-900 line-clamp-1">{item.name || item.display_name.split(',')[0]}</div>
                      <div className="text-[11px] text-slate-500 line-clamp-1">{item.display_name}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {geoError && (
              <div className="p-2.5 bg-amber-50 border border-amber-200 text-amber-800 rounded-xl flex items-center gap-2 text-[11px]">
                <AlertCircle className="w-4 h-4 text-amber-600 shrink-0" />
                <span>{geoError}</span>
              </div>
            )}
          </div>

          {/* Leaflet Interactive Map Container */}
          <div className="relative rounded-2xl overflow-hidden border border-slate-300 shadow-inner group">
            <div 
              ref={mapContainerRef} 
              className="w-full h-64 sm:h-72 z-10" 
              style={{ minHeight: '260px' }}
            />

            {/* Map Status Overlay (Latitude, Longitude Badge & Geocoding status) */}
            <div className="absolute top-3 left-3 z-20 pointer-events-none flex flex-col gap-1.5">
              <div className="bg-slate-900/90 backdrop-blur-md text-white text-[10px] font-mono font-bold px-3 py-1.5 rounded-xl shadow-lg border border-slate-700 flex items-center gap-1.5">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                <span>GPS: {lat.toFixed(6)}, {lng.toFixed(6)}</span>
              </div>

              {isReverseGeocoding && (
                <div className="bg-white/95 backdrop-blur-md text-orange-700 text-[10px] font-bold px-3 py-1 rounded-xl shadow border border-orange-200 flex items-center gap-1.5 animate-pulse">
                  <Loader2 className="w-3 h-3 animate-spin text-orange-600" />
                  <span>Đang định vị địa chỉ...</span>
                </div>
              )}
            </div>

            {/* Hint instruction on map */}
            <div className="absolute bottom-3 left-3 z-20 pointer-events-none hidden sm:block">
              <div className="bg-white/90 backdrop-blur-sm text-slate-600 text-[10px] font-medium px-2.5 py-1 rounded-lg shadow border border-slate-200">
                💡 <em>Click lên bản đồ hoặc kéo thả ghim màu cam để đổi vị trí</em>
              </div>
            </div>
          </div>

          {/* Form Thông Tin Chi Tiết Người Nhận & Địa Chỉ */}
          <form onSubmit={handleSubmit} className="space-y-3 pt-1">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {/* Họ tên người nhận */}
              <div>
                <label className="block text-slate-700 font-bold mb-1">
                  Họ và tên người nhận <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  placeholder="VD: Nguyễn Văn A"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                />
              </div>

              {/* Số điện thoại */}
              <div>
                <label className="block text-slate-700 font-bold mb-1">
                  Số điện thoại liên hệ <span className="text-rose-500">*</span>
                </label>
                <input
                  type="tel"
                  required
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="VD: 0988 123 456"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                />
              </div>

              {/* Số nhà, tên đường (tự động gợi ý từ bản đồ, người dùng sửa tay được) */}
              <div className="sm:col-span-2">
                <label className="block text-slate-700 font-bold mb-1">
                  Địa chỉ cụ thể (Số nhà, ngõ/ngách, tên đường hoặc tòa nhà) <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  value={streetAddress}
                  onChange={(e) => setStreetAddress(e.target.value)}
                  placeholder="VD: Số 18 Ngõ 45 Cầu Giấy, Tòa Dolphin Plaza..."
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                />
              </div>

              {/* Phường / Xã */}
              <div>
                <label className="block text-slate-700 font-bold mb-1">Phường / Xã</label>
                <input
                  type="text"
                  value={ward}
                  onChange={(e) => setWard(e.target.value)}
                  placeholder="VD: Phường Quan Hoa"
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                />
              </div>

              {/* Quận / Huyện & Thành phố */}
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-slate-700 font-bold mb-1">Quận / Huyện</label>
                  <input
                    type="text"
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                    placeholder="VD: Cầu Giấy"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                  />
                </div>
                <div>
                  <label className="block text-slate-700 font-bold mb-1">Tỉnh / Thành phố</label>
                  <input
                    type="text"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Hà Nội"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-xl focus:outline-none focus:border-orange-500 focus:bg-white text-xs"
                  />
                </div>
              </div>
            </div>

            {/* Nhãn địa chỉ & Đặt làm mặc định */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pt-2 border-t border-slate-100">
              <div className="flex items-center gap-2">
                <span className="font-bold text-slate-700">Loại địa chỉ:</span>
                {[
                  { id: 'Nhà riêng', icon: Home },
                  { id: 'Văn phòng', icon: Briefcase },
                  { id: 'Khác', icon: Tag },
                ].map((item) => {
                  const Icon = item.icon;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setTag(item.id)}
                      className={`px-3 py-1.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition ${
                        tag === item.id
                          ? 'bg-orange-500 text-white shadow-sm'
                          : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                      }`}
                    >
                      <Icon className="w-3.5 h-3.5" />
                      <span>{item.id}</span>
                    </button>
                  );
                })}
              </div>

              <label className="flex items-center gap-2 cursor-pointer select-none">
                <input
                  type="checkbox"
                  checked={isDefault}
                  onChange={(e) => setIsDefault(e.target.checked)}
                  className="accent-orange-600 w-4 h-4 cursor-pointer"
                />
                <span className="font-semibold text-slate-700">Đặt làm địa chỉ nhận hàng mặc định</span>
              </label>
            </div>

            {/* Input ẩn lưu trữ Latitude & Longitude theo yêu cầu */}
            <input type="hidden" name="latitude" value={lat || ''} />
            <input type="hidden" name="longitude" value={lng || ''} />

            {/* Modal Actions */}
            <div className="pt-3 flex justify-end gap-2 border-t border-slate-100">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold transition cursor-pointer"
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="px-6 py-2.5 bg-gradient-to-r from-orange-500 to-rose-600 hover:from-orange-600 hover:to-rose-700 text-white font-bold rounded-xl shadow-md transition active:scale-95 flex items-center gap-2 cursor-pointer disabled:opacity-50"
              >
                <Check className="w-4 h-4" />
                <span>{isSubmitting ? 'Đang lưu...' : 'Xác Nhận & Lưu Địa Chỉ'}</span>
              </button>
            </div>
          </form>

        </div>
      </div>
    </div>
  );
}
