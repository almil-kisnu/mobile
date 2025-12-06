-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.detail_diskon_produk (
id_detail_diskon_produk integer NOT NULL DEFAULT nextval('detail_diskon_produk_id_detail_diskon_produk_seq'::regclass),
id_diskon integer NOT NULL,
idproduk integer NOT NULL,
CONSTRAINT detail_diskon_produk_pkey PRIMARY KEY (id_detail_diskon_produk),
CONSTRAINT detail_diskon_produk_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk),
CONSTRAINT detail_diskon_produk_id_diskon_fkey FOREIGN KEY (id_diskon) REFERENCES public.event_diskon(id_diskon)
);
CREATE TABLE public.detail_stock (
id_detail_stock integer NOT NULL DEFAULT nextval('detail_stock_id_detail_stock_seq'::regclass),
idproduk integer NOT NULL,
harga_beli numeric NOT NULL,
stok integer NOT NULL,
tgl_kadaluarsa date,
idoutlet integer NOT NULL DEFAULT 1,
CONSTRAINT detail_stock_pkey PRIMARY KEY (id_detail_stock),
CONSTRAINT fk_produk FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk),
CONSTRAINT detail_stock_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.detail_transfer (
iddetail_transfer integer NOT NULL DEFAULT nextval('detail_transfer_iddetail_transfer_seq'::regclass),
idtransfer integer NOT NULL,
idproduk integer NOT NULL,
jumlah integer NOT NULL,
jumlah_diterima integer,
catatan text,
CONSTRAINT detail_transfer_pkey PRIMARY KEY (iddetail_transfer),
CONSTRAINT detail_transfer_idtransfer_fkey FOREIGN KEY (idtransfer) REFERENCES public.transfer_stock(idtransfer),
CONSTRAINT detail_transfer_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk)
);
CREATE TABLE public.detailopname (
iddetail integer NOT NULL DEFAULT nextval('detailopname_iddetail_seq'::regclass),
idopname integer,
idproduk integer,
stocksistem integer,
stockfisik integer,
selisih integer,
hargabeli numeric,
nilaiselisihrp numeric,
keterangan text,
CONSTRAINT detailopname_pkey PRIMARY KEY (iddetail),
CONSTRAINT detailopname_idopname_fkey FOREIGN KEY (idopname) REFERENCES public.opname(idopname),
CONSTRAINT detailopname_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk)
);
CREATE TABLE public.detailorder (
iddetail integer NOT NULL DEFAULT nextval('detailorder_iddetail_seq'::regclass),
idorder integer NOT NULL,
idproduk integer NOT NULL,
harga numeric NOT NULL,
jumlah integer NOT NULL,
subtotal numeric NOT NULL,
CONSTRAINT detailorder_pkey PRIMARY KEY (iddetail),
CONSTRAINT detailorder_idorder_fkey FOREIGN KEY (idorder) REFERENCES public.orders(idorder),
CONSTRAINT detailorder_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk)
);
CREATE TABLE public.event_diskon (
id_diskon integer NOT NULL DEFAULT nextval('event_diskon_id_diskon_seq'::regclass),
nama_diskon character varying NOT NULL,
deskripsi text,
tanggal_mulai date NOT NULL,
jam_mulai time without time zone NOT NULL,
tanggal_selesai date NOT NULL,
jam_selesai time without time zone NOT NULL,
nilai_diskon integer NOT NULL,
berlaku_untuk USER-DEFINED NOT NULL,
is_active boolean DEFAULT true,
created_at timestamp without time zone DEFAULT now(),
CONSTRAINT event_diskon_pkey PRIMARY KEY (id_diskon)
);
CREATE TABLE public.harga_grosir (
id_harga integer NOT NULL DEFAULT nextval('harga_grosir_id_harga_seq'::regclass),
idproduk integer NOT NULL,
min_qty integer NOT NULL DEFAULT 1,
harga_jual numeric NOT NULL,
CONSTRAINT harga_grosir_pkey PRIMARY KEY (id_harga),
CONSTRAINT fk_produk_harga FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk)
);
CREATE TABLE public.jadwal_mingguan (
id_siklus uuid NOT NULL DEFAULT gen_random_uuid(),
id_pengguna integer NOT NULL,
id_shift_senin integer,
id_shift_selasa integer,
id_shift_rabu integer,
id_shift_kamis integer,
id_shift_jumat integer,
id_shift_sabtu integer,
id_shift_minggu integer,
CONSTRAINT jadwal_mingguan_pkey PRIMARY KEY (id_siklus),
CONSTRAINT siklus_mingguan_id_shift_senin_fkey FOREIGN KEY (id_shift_senin) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_selasa_fkey FOREIGN KEY (id_shift_selasa) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_rabu_fkey FOREIGN KEY (id_shift_rabu) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_kamis_fkey FOREIGN KEY (id_shift_kamis) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_jumat_fkey FOREIGN KEY (id_shift_jumat) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_sabtu_fkey FOREIGN KEY (id_shift_sabtu) REFERENCES public.shift(id_shift_def),
CONSTRAINT siklus_mingguan_id_shift_minggu_fkey FOREIGN KEY (id_shift_minggu) REFERENCES public.shift(id_shift_def),
CONSTRAINT fk_pengguna FOREIGN KEY (id_pengguna) REFERENCES public.pengguna(iduser)
);
CREATE TABLE public.kartustock (
idkartustock integer NOT NULL DEFAULT nextval('kartustock_idkartustock_seq'::regclass),
idproduk integer,
jenistransaksi character varying CHECK (jenistransaksi::text = ANY (ARRAY['masuk'::character varying, 'keluar'::character varying, 'retur'::character varying, 'rusak'::character varying]::text[])),
jumlah integer,
stockawal integer,
stockakhir integer,
tanggal date DEFAULT CURRENT_DATE,
idoutlet integer NOT NULL DEFAULT 1,
CONSTRAINT kartustock_pkey PRIMARY KEY (idkartustock),
CONSTRAINT kartustock_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk),
CONSTRAINT kartustock_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.kategori (
idkategori integer NOT NULL DEFAULT nextval('kategori_idkategori_seq'::regclass),
nkategori character varying NOT NULL,
CONSTRAINT kategori_pkey PRIMARY KEY (idkategori)
);
CREATE TABLE public.laporan_shift (
id_lap_shift integer NOT NULL DEFAULT nextval('laporan_shift_id_lap_shift_seq'::regclass),
tanggal date NOT NULL DEFAULT CURRENT_DATE,
idoutlet integer NOT NULL DEFAULT 1,
id_shift_def integer NOT NULL,
metode_pembayaran character varying NOT NULL,
total numeric NOT NULL DEFAULT 0,
uangfisik numeric,
selisih numeric,
catatan text,
idkasir integer,
created_at timestamp without time zone DEFAULT now(),
updated_at timestamp without time zone DEFAULT now(),
CONSTRAINT laporan_shift_pkey PRIMARY KEY (id_lap_shift)
);
CREATE TABLE public.opname (
idopname integer NOT NULL DEFAULT nextval('opname_idopname_seq'::regclass),
tanggalopname date DEFAULT CURRENT_DATE,
iduserpetugas integer,
totalselisihrp numeric,
idoutlet integer NOT NULL DEFAULT 1,
CONSTRAINT opname_pkey PRIMARY KEY (idopname),
CONSTRAINT opname_iduserpetugas_fkey FOREIGN KEY (iduserpetugas) REFERENCES public.pengguna(iduser),
CONSTRAINT opname_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.orders (
idorder integer NOT NULL DEFAULT nextval('order_idorder_seq'::regclass),
namapelanggan character varying NOT NULL DEFAULT 'Umum'::character varying,
grandtotal numeric NOT NULL,
bayar numeric NOT NULL,
kembalian numeric NOT NULL,
idkasir integer,
tanggalorder timestamp without time zone NOT NULL DEFAULT now(),
idoutlet integer NOT NULL DEFAULT 1,
metode_pembayaran USER-DEFINED NOT NULL DEFAULT 'transfer'::metode_pembayaran_enum,
status USER-DEFINED NOT NULL DEFAULT 'aman'::status_enum,
notelp character varying,
alamat character varying,
CONSTRAINT orders_pkey PRIMARY KEY (idorder),
CONSTRAINT order_idkasir_fkey FOREIGN KEY (idkasir) REFERENCES public.pengguna(iduser),
CONSTRAINT orders_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.otp_failed_attempts (
id integer NOT NULL DEFAULT nextval('otp_failed_attempts_id_seq'::regclass),
phone character varying NOT NULL,
attempts integer DEFAULT 0,
last_attempt timestamp with time zone,
blocked_until timestamp with time zone,
created_at timestamp with time zone DEFAULT now(),
CONSTRAINT otp_failed_attempts_pkey PRIMARY KEY (id)
);
CREATE TABLE public.outlet (
idoutlet integer NOT NULL DEFAULT nextval('outlet_idoutlet_seq'::regclass),
kode_outlet character varying NOT NULL UNIQUE,
nama_outlet character varying NOT NULL,
alamat text,
telepon character varying,
is_active boolean DEFAULT true,
created_at timestamp without time zone DEFAULT now(),
CONSTRAINT outlet_pkey PRIMARY KEY (idoutlet)
);
CREATE TABLE public.password_reset_otp (
id bigint NOT NULL DEFAULT nextval('password_reset_otp_id_seq'::regclass),
phone character varying NOT NULL,
otp_code character varying NOT NULL,
expires_at timestamp without time zone NOT NULL,
is_used boolean DEFAULT false,
created_at timestamp without time zone DEFAULT now(),
CONSTRAINT password_reset_otp_pkey PRIMARY KEY (id)
);
CREATE TABLE public.pengeluaran (
idpengeluaran integer NOT NULL DEFAULT nextval('pengeluaran_idpengeluaran_seq'::regclass),
tanggal date NOT NULL DEFAULT CURRENT_DATE,
deskripsi character varying NOT NULL,
jumlah numeric NOT NULL,
kategori character varying,
iduser integer,
idoutlet integer NOT NULL DEFAULT 1,
created_at timestamp without time zone DEFAULT now(),
id_shift_def integer,
CONSTRAINT pengeluaran_pkey PRIMARY KEY (idpengeluaran),
CONSTRAINT pengeluaran_iduser_fkey FOREIGN KEY (iduser) REFERENCES public.pengguna(iduser),
CONSTRAINT pengeluaran_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet),
CONSTRAINT pengeluaran_id_shift_def_fkey FOREIGN KEY (id_shift_def) REFERENCES public.shift(id_shift_def)
);
CREATE TABLE public.pengguna (
iduser integer NOT NULL DEFAULT nextval('users_iduser_seq'::regclass),
username character varying NOT NULL,
password character varying,
createdat timestamp without time zone DEFAULT now(),
phone character varying UNIQUE,
is_active boolean DEFAULT true,
deactivated_at date,
deactivated_reason character varying,
hired_date date DEFAULT CURRENT_DATE,
updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
idoutlet integer,
nik character varying,
role USER-DEFINED,
CONSTRAINT pengguna_pkey PRIMARY KEY (iduser),
CONSTRAINT pengguna_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.pre_orders (
id_pre_order bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
midtrans_order_id text NOT NULL,
order_data_json jsonb NOT NULL,
created_at timestamp with time zone NOT NULL DEFAULT now(),
status text NOT NULL DEFAULT 'pending'::text,
CONSTRAINT pre_orders_pkey PRIMARY KEY (id_pre_order)
);
CREATE TABLE public.presensi (
id uuid NOT NULL DEFAULT gen_random_uuid(),
tanggal date NOT NULL,
jam_masuk time without time zone NOT NULL,
jam_pulang time without time zone,
status USER-DEFINED NOT NULL,
keterangan_izin text,
pesan_owner text,
created_at timestamp with time zone DEFAULT now(),
id_pengguna integer,
id_shift_def integer,
waktu_kerja text,
shift text,
lokasi text,
CONSTRAINT presensi_pkey PRIMARY KEY (id),
CONSTRAINT absensi_id_pengguna_fkey FOREIGN KEY (id_pengguna) REFERENCES public.pengguna(iduser),
CONSTRAINT absensi_id_shift_def_fkey FOREIGN KEY (id_shift_def) REFERENCES public.shift(id_shift_def)
);
CREATE TABLE public.produk (
idproduk integer NOT NULL DEFAULT nextval('produk_idproduk_seq'::regclass),
namaproduk character varying NOT NULL,
idkategori integer,
gambar character varying,
deskripsi text,
status character varying DEFAULT 'aktif'::character varying CHECK (status::text = ANY (ARRAY['aktif'::character varying, 'nonaktif'::character varying]::text[])),
barcode character varying,
harga_eceran numeric,
CONSTRAINT produk_pkey PRIMARY KEY (idproduk),
CONSTRAINT produk_idkategori_fkey FOREIGN KEY (idkategori) REFERENCES public.kategori(idkategori)
);
CREATE TABLE public.rusak (
idrusak integer NOT NULL DEFAULT nextval('rusak_idrusak_seq'::regclass),
idproduk integer,
jumlah integer,
alasan USER-DEFINED,
nilaikerugian numeric,
tanggal date DEFAULT CURRENT_DATE,
idoutlet integer NOT NULL DEFAULT 1,
status_verifikasi USER-DEFINED NOT NULL DEFAULT 'Pending'::status_verifikasi_rusak_enum,
CONSTRAINT rusak_pkey PRIMARY KEY (idrusak),
CONSTRAINT rusak_idproduk_fkey FOREIGN KEY (idproduk) REFERENCES public.produk(idproduk),
CONSTRAINT rusak_idoutlet_fkey FOREIGN KEY (idoutlet) REFERENCES public.outlet(idoutlet)
);
CREATE TABLE public.shift (
id_shift_def integer NOT NULL DEFAULT nextval('shift_definition_id_shift_def_seq'::regclass),
nama_shift character varying NOT NULL,
jam_mulai time without time zone NOT NULL,
jam_selesai time without time zone NOT NULL,
CONSTRAINT shift_pkey PRIMARY KEY (id_shift_def)
);
CREATE TABLE public.transfer_stock (
idtransfer integer NOT NULL DEFAULT nextval('transfer_stock_idtransfer_seq'::regclass),
idoutlet_asal integer,
idoutlet_tujuan integer NOT NULL,
tanggal_transfer date DEFAULT CURRENT_DATE,
tanggal_terima date,
status USER-DEFINED DEFAULT 'pending'::transfer_status_enum,
iduser_pengirim integer,
iduser_penerima integer,
catatan text,
iduser_peminta integer,
CONSTRAINT transfer_stock_pkey PRIMARY KEY (idtransfer),
CONSTRAINT transfer_idoutlet_asal_fkey FOREIGN KEY (idoutlet_asal) REFERENCES public.outlet(idoutlet),
CONSTRAINT transfer_idoutlet_tujuan_fkey FOREIGN KEY (idoutlet_tujuan) REFERENCES public.outlet(idoutlet),
CONSTRAINT transfer_iduser_pengirim_fkey FOREIGN KEY (iduser_pengirim) REFERENCES public.pengguna(iduser),
CONSTRAINT transfer_iduser_penerima_fkey FOREIGN KEY (iduser_penerima) REFERENCES public.pengguna(iduser),
CONSTRAINT transfer_iduser_peminta_fkey FOREIGN KEY (iduser_peminta) REFERENCES public.pengguna(iduser)
);
